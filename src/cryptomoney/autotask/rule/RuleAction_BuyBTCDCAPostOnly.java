/*
    This file is part of cryptomoney-autotask.

    cryptomoney-autotask is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    cryptomoney-autotask is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with cryptomoney-autotask.  If not, see <https://www.gnu.org/licenses/>.
 */
package cryptomoney.autotask.rule;

import com.coinbase.exchange.api.entity.CryptoPaymentRequest;
import com.coinbase.exchange.api.orders.*;
import com.coinbase.exchange.api.accounts.Account;

import cryptomoney.autotask.CryptomoneyAutotask;
import cryptomoney.autotask.allowance.*;
import cryptomoney.autotask.functions.SharedFunctions;
import cryptomoney.autotask.currency.CoinCurrencyType;
import cryptomoney.autotask.currency.FiatCurrencyType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Dollar cost averaging coinbase pro using post-only method (most complicated but 0% fee)
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAction_BuyBTCDCAPostOnly extends Rule
{
    private CoinCurrencyType coinCurrencyType;
    private FiatCurrencyType fiatCurrencyType;
    private double maximumAvgOccurrencesPerDay;
    private double minimumQuantityBuyUSD;
    private double minimumQuantityCoinThreshold;
    private double maximumQuantityCoin;
    private double randomChanceToProceed;
    private int executionCount = 0; //if we set this to 999999 then it would execute right away upon running program (maybe)

    
    public RuleAction_BuyBTCDCAPostOnly()
    {
        super(RuleType.ACTION, ActionType.ACTION_BUY_BTC_DCA_POSTONLY);
    }
    
    /**
     * Places buy orders (post-only orders which never execute immediately) based on different intervals:
     *   1) allowance, wait for enough allowance to build up
     *   2) ensure a number of doAction executions have occurred (based on an average number per day target)
     *   3) add a random delay, e.g. 0.15 percent chance to proceed may delay 7 times (5 seconds each)
     * 
     * @param _maximumAvgOccurrencesPerDay
     * @param _minimumQuantityBuyUSD
     * @param _minimumQuantityCoinThreshold
     * @param _maximumQuantityCoin
     * @param _randomChanceToProceed  decimal between 0 and 1, e.g. 25% change is 0.25.  This only delays it until the next INTERVAL (e.g. 5 seconds), not even next execution.  Does not reset any other timers.
     */
    public RuleAction_BuyBTCDCAPostOnly(CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType, boolean _executeImmediately, double _maximumAvgOccurrencesPerDay, double _minimumQuantityBuyUSD, double _minimumQuantityCoinThreshold, double _maximumQuantityCoin, double _randomChanceToProceed)
    {
        super(RuleType.ACTION, ActionType.ACTION_BUY_BTC_DCA_POSTONLY);
        coinCurrencyType = _coinCurrencyType;
        fiatCurrencyType = _fiatCurrencyType;
        maximumAvgOccurrencesPerDay = _maximumAvgOccurrencesPerDay;
        minimumQuantityBuyUSD = _minimumQuantityBuyUSD;
        minimumQuantityCoinThreshold = _minimumQuantityCoinThreshold;
        maximumQuantityCoin = _maximumQuantityCoin;
        randomChanceToProceed = _randomChanceToProceed;
        
        if(_executeImmediately)
        {
            executionCount = (int)Math.ceil(getNumberOfExecutionsBeforeExecutingOnce());
        }        
    }
    
    private double getNumberOfExecutionsBeforeExecutingOnce()
    {
        double systemExecutionsPerDay = SharedFunctions.GetNumberOfSystemIntervalsPerDay();
        double numberOfExecutionsBeforeExecutingOnce = systemExecutionsPerDay / maximumAvgOccurrencesPerDay;
        return numberOfExecutionsBeforeExecutingOnce;
    }
    
    private AllowanceCoinFiat getAssociatedAllowance()
    {
        return this.account.getAllowanceCoinFiat(AllowanceType.Buy, coinCurrencyType, fiatCurrencyType);
    }
    
    @Override
    public void doAction()
    {
        //CryptomoneyAutotask.logProv.LogMessage(getHelpString());
        
        executionCount++;
        
        if(getAssociatedAllowance().getAllowance().doubleValue() <= this.minimumQuantityBuyUSD)
        {
            CryptomoneyAutotask.logProv.LogMessage(getHelpString() + " coinAmountToPurchaseRough does not exceed this.minimumQuantityCoinThreshold " + getAssociatedAllowance().getAllowance().setScale(2, RoundingMode.HALF_EVEN) + "/" + this.minimumQuantityBuyUSD);
            return;
        }
        
        
        double numberOfExecutionsBeforeExecutingOnce = getNumberOfExecutionsBeforeExecutingOnce();
        
        CryptomoneyAutotask.logProv.LogMessage(getHelpString() + " execution count: " + executionCount + "/" + numberOfExecutionsBeforeExecutingOnce);
        if(executionCount < numberOfExecutionsBeforeExecutingOnce)
        {
            //keep waiting...
            return;
        }
        else
        {
            if(SharedFunctions.RollDie(randomChanceToProceed, CryptomoneyAutotask.logProv))
            {
                executionCount-=numberOfExecutionsBeforeExecutingOnce;

                for(int i=0;i<100 && executionCount > numberOfExecutionsBeforeExecutingOnce;i++)
                {
                    if(executionCount > numberOfExecutionsBeforeExecutingOnce)
                    {
                        executionCount-=numberOfExecutionsBeforeExecutingOnce; //don't let it run a bunch of times in a row
                    }
                }
            }
            else
            {
                //random wait, don't account for it happening less often, just sometimes don't do it, to make it less predictable for "enemy" bots.
                CryptomoneyAutotask.logProv.LogMessage("random delay...");
                return;
            }
        }
        
        
        //PROBABLY PROCEEDING WITH BUY
        
        BigDecimal BTCPriceInUSD= SharedFunctions.GetBestCoinBuyPrice(this.coinCurrencyType, this.fiatCurrencyType).setScale(10, RoundingMode.HALF_EVEN); //API call
        
        BigDecimal coinAmountToPurchase;
        if(true) //this is just to reduce scope
        { 
            
            BigDecimal allowance = getAssociatedAllowance().getAllowance().setScale(10, RoundingMode.HALF_EVEN);
            CryptomoneyAutotask.logProv.LogMessage("best BTC buy price: " + BTCPriceInUSD.toString());
            CryptomoneyAutotask.logProv.LogMessage("allowanceBuyBTCinUSD: " + allowance);

            coinAmountToPurchase = allowance.divide(BTCPriceInUSD, RoundingMode.HALF_EVEN).setScale(8, RoundingMode.HALF_EVEN); //BTC max resolution is .00000001
        }
        //TODO: sanity check, don't let price be too far above 30 day average (or something).

        //todo: setting for max # of orders, currently it only allows 1 because that's a lot easier to track.

        //TODO: refactor this. can put more logic here, weird logic in exchange account and shaerd functions
        
        if(coinAmountToPurchase.doubleValue() >= this.minimumQuantityCoinThreshold)
        {
            //DEFINITELY PROBABLY PROCEED WITH BUY
            

            
            //todo: do this as a separate action, maybe like a separate rule
            boolean cancelAnyOpenOrders = true;
            boolean changedAllowance = this.account.ProcessBuyOrders(coinCurrencyType, fiatCurrencyType, cancelAnyOpenOrders); //API call
            if(changedAllowance) 
            {
                BigDecimal allowance = getAssociatedAllowance().getAllowance().setScale(10, RoundingMode.HALF_EVEN);
                coinAmountToPurchase = allowance.divide(
                        BTCPriceInUSD.setScale(10, RoundingMode.HALF_EVEN), 
                        RoundingMode.HALF_EVEN
                    ).setScale(8, RoundingMode.HALF_EVEN); //temporary, might increase amount to buy if ours was cancelled or only partially filled or something
            }

            if(coinAmountToPurchase.doubleValue() > maximumQuantityCoin)
            {
               coinAmountToPurchase = BigDecimal.valueOf(maximumQuantityCoin).setScale(8, RoundingMode.HALF_EVEN);
            }
            
            //make sure we have sufficient funds
            BigDecimal estimatedCostOfBuy = coinAmountToPurchase.multiply(BTCPriceInUSD);
            estimatedCostOfBuy = estimatedCostOfBuy.multiply(new BigDecimal(1.003));
            Account usdAcct = this.account.getCoinbaseProUSDAccount();
            if(usdAcct.getAvailable().doubleValue() < estimatedCostOfBuy.doubleValue()) //asume we might need to pay the 0.3% fee (only in some situations
            {
                String logInfo = "insufficient funds to buy, not proceeding " + usdAcct.getAvailable().doubleValue() + " < " + estimatedCostOfBuy.doubleValue();
                CryptomoneyAutotask.logMultiplexer.LogMessage(logInfo);
                return;
            }
            
            
            //DEFINITELY PROCEEDING WITH BUY
            
            Order order = null;
            if(this.account.btcBuyFrequencyDesperation < this.account.BTC_BUY_FREQUENCY_DESPERATION_THRESHOLD)
            {
                //EXECUTE BUY - POST (does not execute immediately
                CryptomoneyAutotask.logProv.LogMessage("coin amount purchase triggered (post-only): " + CryptomoneyAutotask.btcFormat.format(coinAmountToPurchase) + "/" + this.minimumQuantityCoinThreshold);      
                order = this.account.buyCoinPostOnly(coinAmountToPurchase, coinCurrencyType, fiatCurrencyType); //API call
            }
            else
            {
                //DESPARATE BUY - BUY IMMEDIATELY (within reason)
                CryptomoneyAutotask.logProv.LogMessage("coin amount purchase triggered (immediate): " + CryptomoneyAutotask.btcFormat.format(coinAmountToPurchase) + "/" + this.minimumQuantityCoinThreshold);      
                order = this.account.buyCoinImmediate(coinAmountToPurchase, coinCurrencyType, fiatCurrencyType); //API call                
            }
            
            if(order != null)
            {
                boolean postOnly = Boolean.parseBoolean(order.getPost_only());
                String desperationInfo = " desp: " + this.account.btcBuyFrequencyDesperation + "/" + this.account.BTC_BUY_FREQUENCY_DESPERATION_THRESHOLD;
                String postOnlyInfo = "postType " + (postOnly ? "postonly" : "immediate");
                String logString = "Placed order " + order.toString() + " " + desperationInfo + " " + postOnlyInfo + " estUsd: " + estimatedCostOfBuy.doubleValue();
                CryptomoneyAutotask.logMultiplexer.LogMessage(logString);
                getAssociatedAllowance().addToAllowance(estimatedCostOfBuy.negate());
                
                //purge any extra allowance
                if(getAssociatedAllowance().getAllowance().doubleValue() > 0)
                {
                    CryptomoneyAutotask.logMultiplexer.LogMessage("purging USD BTC BUY allowance " + getAssociatedAllowance().getAllowance());
                    getAssociatedAllowance().resetAllowance();
                }
            }
            else
            {
                CryptomoneyAutotask.logMultiplexer.LogMessage("buy order failed");
            }
            
            
        }
        else
        {
            CryptomoneyAutotask.logProv.LogMessage("coin amount to purchase less than threshold: " + CryptomoneyAutotask.btcFormat.format(coinAmountToPurchase) + "/" + this.minimumQuantityCoinThreshold);      
        }
        
        
        CryptomoneyAutotask.logProv.LogMessage("");
    }
            
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType() 
            + " maximumAvgOccurrencesPerDay:" + maximumAvgOccurrencesPerDay
            + " minimumQuantityBuyUSD:" + minimumQuantityBuyUSD
            + " minimumQuantityCoinThreshold:" + minimumQuantityCoinThreshold
            + " maximumQuantityCoin:" + maximumQuantityCoin
            + " randomChanceToProceed:" + randomChanceToProceed
                ;
        
    }
}
