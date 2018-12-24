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

import com.coinbase.exchange.api.accounts.Account;
import cryptomoney.autotask.CryptomoneyAutotask;
import com.coinbase.exchange.api.payments.CoinbaseAccount;
import com.coinbase.exchange.api.entity.PaymentResponse;
import com.coinbase.exchange.api.marketdata.MarketData;
import com.coinbase.exchange.api.marketdata.OrderItem;
import cryptomoney.autotask.allowance.AllowanceCoinFiat;
import cryptomoney.autotask.allowance.AllowanceType;
import cryptomoney.autotask.currency.CoinCurrencyType;
import cryptomoney.autotask.currency.FiatCurrencyType;
import cryptomoney.autotask.functions.SharedFunctions;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAction_WithdrawBTCToCoinbase extends Rule
{
    private CoinCurrencyType coinCurrencyType;
    private FiatCurrencyType fiatCurrencyType;
    private double maximumAvgOccurrencesPerDay;
    private double minimumUSDQuantityThreshold;
    private double maximumUSDQuantity;
    
    private int executionCount = 0; //if we set this to 999999 then it would execute right away upon running program (maybe)
    
    public RuleAction_WithdrawBTCToCoinbase()
    {
        super(RuleType.ACTION, ActionType.ACTION_WITHDRAW_BTC_TO_COINBASE);
    }
    
    public RuleAction_WithdrawBTCToCoinbase(CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType, boolean _executeImmediately, double _maximumAvgOccurrencesPerDay, double _minimumUSDQuantityThreshold, double _maximumUSDQuantity)
    {
        super(RuleType.ACTION, ActionType.ACTION_WITHDRAW_BTC_TO_COINBASE);
        coinCurrencyType = _coinCurrencyType;
        fiatCurrencyType = _fiatCurrencyType;
        maximumAvgOccurrencesPerDay = _maximumAvgOccurrencesPerDay;
        minimumUSDQuantityThreshold = _minimumUSDQuantityThreshold;
        maximumUSDQuantity = _maximumUSDQuantity;
        
        if(_executeImmediately)
        {
            executionCount = (int)Math.ceil(getNumberOfExecutionsBeforeExecutingOnce());
        }
        
    }
    
    private AllowanceCoinFiat getAssociatedAllowance()
    {
        return this.account.getAllowanceCoinFiat(AllowanceType.WithdrawCoinToCoinbase, coinCurrencyType, fiatCurrencyType);
    }
    
    private double getNumberOfExecutionsBeforeExecutingOnce()
    {
        double systemExecutionsPerDay = SharedFunctions.GetNumberOfSystemIntervalsPerDay();
        double numberOfExecutionsBeforeExecutingOnce = systemExecutionsPerDay / maximumAvgOccurrencesPerDay;
        return numberOfExecutionsBeforeExecutingOnce;
    }
    
    @Override
    public void doAction()
    {
        //CryptomoneyAutotask.logProv.LogMessage(getHelpString());
        
        executionCount++;

        
        
        if(getAssociatedAllowance().getAllowance().doubleValue() < minimumUSDQuantityThreshold)
        {
            CryptomoneyAutotask.logProv.LogMessage(getHelpString() + " account.getAllowanceWithdrawBTCToCoinbaseInUSD() does not exceed minimumUSDQuantityThreshold " + getAssociatedAllowance().getAllowance() + "/" + minimumUSDQuantityThreshold);
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
            executionCount-=numberOfExecutionsBeforeExecutingOnce;
            
            for(int i=0;i<100 && executionCount > numberOfExecutionsBeforeExecutingOnce;i++)
            {
                if(executionCount > numberOfExecutionsBeforeExecutingOnce)
                {
                    executionCount-=numberOfExecutionsBeforeExecutingOnce; //don't let it run a bunch of times in a row
                }
            }
        }
        
        
        
        BigDecimal coinPrice = SharedFunctions.GetBestCoinBuyPrice(coinCurrencyType, fiatCurrencyType);
        
        Account btcAccount = this.account.getCoinbaseProBTCAccount(); //TODO, make generic
        
        if(btcAccount == null)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR, NO BTC ACCOUNT FOUND");
            System.exit(1);
        }
        
        double usdBalanceValueOfBTCInCoinbasePRO = 0;
        BigDecimal btcAvail = btcAccount.getAvailable();
        double valBtcAvail = 0;
        
        if(btcAvail != null)
        { 
            valBtcAvail = btcAvail.doubleValue();
            usdBalanceValueOfBTCInCoinbasePRO = valBtcAvail*coinPrice.doubleValue();
        }
        
        if(valBtcAvail > 0)
        {
            
            if(usdBalanceValueOfBTCInCoinbasePRO >= minimumUSDQuantityThreshold)
            {

                CryptomoneyAutotask.logProv.LogMessage("actiontype: " + getActionType().toString());

                BigDecimal btcToWithdraw = getAssociatedAllowance().getAllowance().divide(coinPrice, 8, RoundingMode.UP);

                //withdraw less if we have less available
                if(btcToWithdraw.doubleValue() > valBtcAvail)
                {
                    btcToWithdraw = new BigDecimal(valBtcAvail).setScale(8, RoundingMode.FLOOR);
                }
                
                double maxBTCwithdrawQuantity = maximumUSDQuantity/coinPrice.doubleValue();
                
                //don't withdraw more than the max configured
                if(btcToWithdraw.doubleValue() > maxBTCwithdrawQuantity)
                {
                    btcToWithdraw = new BigDecimal(maxBTCwithdrawQuantity).setScale(8, RoundingMode.FLOOR);
                }

                String btcCoinbaseAccount_Id = this.account.getCoinbaseRegularBTCAccount_Id();

                if(btcCoinbaseAccount_Id == null)
                {
                    CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR BTC coinbase account not found");
                    System.exit(1);
                }

                //BigDecimal bdBTCAmountToWithdraw = BigDecimal.valueOf(btcToWithdraw).setScale(8, RoundingMode.HALF_EVEN);
                BigDecimal estimatedUSDAmountOfWithdrawal = btcToWithdraw.multiply(coinPrice).setScale(2, RoundingMode.UP);
                
                PaymentResponse response = CryptomoneyAutotask.withdrawalsService.makeWithdrawalToCoinbase(btcToWithdraw, "BTC", btcCoinbaseAccount_Id); //API CALL

                String logString = "Requested BTC withdrawal, response: " + response.getId() + " " + response.getCurrency() + " " + response.getAmount().doubleValue() + " " + response.getPayout_at() + " estimated USD value: " + estimatedUSDAmountOfWithdrawal;
                CryptomoneyAutotask.logMultiplexer.LogMessage(logString);


                getAssociatedAllowance().addToAllowance(estimatedUSDAmountOfWithdrawal.negate());

                //purge any extra allowance
                if(getAssociatedAllowance().getAllowance().doubleValue() > 0)
                {
                    CryptomoneyAutotask.logMultiplexer.LogMessage("purging BTC withdraw allowance " + getAssociatedAllowance().getAllowance());
                    getAssociatedAllowance().resetAllowance();
                }
            }
            else
            {
                CryptomoneyAutotask.logProv.LogMessage("usdBalanceValueOfBTCInCoinbasePRO less than minimumUSDQuantityThreshold");
            }
        }
        else
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("BTC available for withdraw is 0");
        }
        
        CryptomoneyAutotask.logProv.LogMessage("");
    }
    
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType() 
            + " maximumAvgOccurrencesPerDay:" + maximumAvgOccurrencesPerDay
            + " minimumUSDQuantityThreshold:" + minimumUSDQuantityThreshold
            + " maximumUSDQuantity:" + maximumUSDQuantity
                ;
    }
}

