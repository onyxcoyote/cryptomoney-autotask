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

import cryptomoney.autotask.CryptomoneyAutotask;
import cryptomoney.autotask.functions.SharedFunctions;
import java.math.BigDecimal;

/**
 * Dollar cost averaging coinbase pro using post-only method (most complicated but 0% fee)
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAction_BuyBTCDCAPostOnly extends Rule
{
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
     * 
     * @param _maximumAvgOccurrencesPerDay
     * @param _minimumQuantityBuyUSD
     * @param _minimumQuantityCoinThreshold
     * @param _maximumQuantityCoin
     * @param _randomChanceToProceed  decimal between 0 and 1, e.g. 25% change is 0.25
     */
    public RuleAction_BuyBTCDCAPostOnly(boolean _executeImmediately, double _maximumAvgOccurrencesPerDay, double _minimumQuantityBuyUSD, double _minimumQuantityCoinThreshold, double _maximumQuantityCoin, double _randomChanceToProceed)
    {
        super(RuleType.ACTION, ActionType.ACTION_DEPOSIT_USD);
        maximumAvgOccurrencesPerDay = _maximumAvgOccurrencesPerDay;
        minimumQuantityBuyUSD = _minimumQuantityBuyUSD;
        minimumQuantityCoinThreshold = _minimumQuantityCoinThreshold;
        maximumQuantityCoin = _maximumQuantityCoin;
        randomChanceToProceed = _randomChanceToProceed;
        
        if(_executeImmediately)
        {
            executionCount = 999999999;
        }        
    }
    
    @Override
    public void doAction()
    {
        CryptomoneyAutotask.logProv.LogMessage(getHelpString());
        
        executionCount++;
        //TODO: MAKE SURE SUFFICIENT BALANCE TO BUY
        
        if(this.account.getAllowanceBuyBTCinUSD() <= this.minimumQuantityBuyUSD)
        {
            CryptomoneyAutotask.logProv.LogMessage("coinAmountToPurchaseRough does not exceed this.minimumQuantityCoinThreshold " + this.account.getAllowanceBuyBTCinUSD() + "/" + this.minimumQuantityBuyUSD);
            return;
        }
        
        
        double systemExecutionsPerDay = SharedFunctions.GetNumberOfSystemIntervalsPerDay();
        double numberOfExecutionsBeforeExecutingOnce = systemExecutionsPerDay / maximumAvgOccurrencesPerDay;
        
        CryptomoneyAutotask.logProv.LogMessage("execution count: " + executionCount + "/" + numberOfExecutionsBeforeExecutingOnce);
        if(executionCount < numberOfExecutionsBeforeExecutingOnce)
        {
            //keep waiting...
            return;
        }
        else
        {
            if(SharedFunctions.RollDie(randomChanceToProceed))
            {
                executionCount = 0;
            }
            else
            {
                //random wait, don't account for it happening less often, just sometimes don't do it, to make it less predictable for "enemy" bots.
                return;
            }
        }
        
        
        BigDecimal BTCPriceInUSD= SharedFunctions.GetBestBTCBuyPrice(); //API call
        double coinAmountToPurchase = this.account.getAllowanceBuyBTCinUSD()/BTCPriceInUSD.doubleValue();

        //TODO: sanity check, don't let price be too far above 30 day average (or something).

        //todo: setting for max # of orders, currently it only allows 1 because that's a lot easier to track.

        if(coinAmountToPurchase >= this.minimumQuantityCoinThreshold)
        {
            
            //todo: do this as a separate action, maybe like a separate rule
            boolean cancelAnyOpenOrders = true;
            boolean changedAllowance = this.account.ProcessBTCBuyOrders(cancelAnyOpenOrders); //API call
            if(changedAllowance) 
            {
                coinAmountToPurchase = this.account.getAllowanceBuyBTCinUSD()/BTCPriceInUSD.doubleValue(); //temporary, might increase amount to buy if ours was cancelled or only partially filled or something
            }

            if(coinAmountToPurchase > maximumQuantityCoin)
            {
               coinAmountToPurchase = maximumQuantityCoin;
            }
            
            //EXECUTE BUY
            CryptomoneyAutotask.logProv.LogMessage("coin amount purchase triggered: " + CryptomoneyAutotask.btcFormat.format(coinAmountToPurchase) + "/" + this.minimumQuantityCoinThreshold);      
            this.account.buyBTCPostOnly(coinAmountToPurchase); //API call
            
            //purge any extra allowance
            this.account.resetAllowanceBuyBTCinUSD();
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
        return this.getRuleType() + " " + this.getActionType() + "";
    }
}
