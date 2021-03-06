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
import cryptomoney.autotask.exchangeaccount.WalletAccountCurrency;
import cryptomoney.autotask.functions.SharedFunctions;

import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAction_WithdrawCoinToCoinbase extends Rule
{
    private CoinCurrencyType coinCurrencyType;
    private FiatCurrencyType fiatCurrencyType;
    private double maximumAvgOccurrencesPerDay;
    private double minimumFiatQuantityThreshold;
    private double maximumCurrencyQuantity;
    
    private int executionCount = 0; //if we set this to 999999 then it would execute right away upon running program (maybe)
    
    public RuleAction_WithdrawCoinToCoinbase()
    {
        super(null, RuleType.ACTION, ActionType.ACTION_WITHDRAW_COIN_TO_COINBASE);
    }
    
    public RuleAction_WithdrawCoinToCoinbase(UUID _uuid, CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType, boolean _executeImmediately, double _maximumAvgOccurrencesPerDay, double _minimumCurrencyQuantityThreshold, double _maximumCurrencyQuantity)
    {
        super(_uuid, RuleType.ACTION, ActionType.ACTION_WITHDRAW_COIN_TO_COINBASE);
        coinCurrencyType = _coinCurrencyType;
        fiatCurrencyType = _fiatCurrencyType;
        maximumAvgOccurrencesPerDay = _maximumAvgOccurrencesPerDay;
        minimumFiatQuantityThreshold = _minimumCurrencyQuantityThreshold;
        maximumCurrencyQuantity = _maximumCurrencyQuantity;
        
        if(_executeImmediately)
        {
            executionCount = (int)Math.ceil(getNumberOfExecutionsBeforeExecutingOnce());
        }
        
    }
    
    private AllowanceCoinFiat getAssociatedAllowance()
    {
        return this.account.getAllowanceCoinFiat(AllowanceType.WithdrawCoinToCoinbase, coinCurrencyType, fiatCurrencyType, this.uuid);
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

        
        
        if(getAssociatedAllowance().getAllowance().doubleValue() < minimumFiatQuantityThreshold)
        {
            CryptomoneyAutotask.logProv.LogMessage(getHelpString() + " account.getAllowanceWithdrawCoinToCoinbaseInFiat() does not exceed minimumFiatQuantityThreshold " + getAssociatedAllowance().getAllowance().setScale(3, RoundingMode.HALF_EVEN) + "/" + minimumFiatQuantityThreshold);
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
            
            if(executionCount > numberOfExecutionsBeforeExecutingOnce)
            {
                executionCount = (int)Math.round(executionCount % numberOfExecutionsBeforeExecutingOnce); //modulo - reduces it to a number less than numberOfExecutionsBeforeExecutingOnce to prevent it from running thrice (or more) in sequence in case the executionCount built up a lot.
            }
        }
        
        
        
        BigDecimal coinPrice = SharedFunctions.GetBestCoinBuyPrice(coinCurrencyType, fiatCurrencyType);
        
        Account btcAccount = this.account.getCoinbaseProWalletAccount(WalletAccountCurrency.valueOf(this.coinCurrencyType.toString()));
        
        if(btcAccount == null)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR, NO COIN ACCOUNT FOUND");
            System.exit(1);
        }
        
        double fiatBalanceValueOfCoinInCoinbasePRO = 0;
        BigDecimal coinQuantityAvail = btcAccount.getAvailable();
        double valCoinAvail = 0;
        
        if(coinQuantityAvail != null)
        { 
            valCoinAvail = coinQuantityAvail.doubleValue();
            fiatBalanceValueOfCoinInCoinbasePRO = valCoinAvail*coinPrice.doubleValue();
        }
        
        if(valCoinAvail > 0)
        {
            
            if(fiatBalanceValueOfCoinInCoinbasePRO >= minimumFiatQuantityThreshold)
            {

                CryptomoneyAutotask.logProv.LogMessage("actiontype: " + getActionType().toString());

                BigDecimal coinQuantityToWithdraw = getAssociatedAllowance().getAllowance().divide(coinPrice, 8, RoundingMode.UP);

                //withdraw less if we have less available
                if(coinQuantityToWithdraw.doubleValue() > valCoinAvail)
                {
                    coinQuantityToWithdraw = new BigDecimal(valCoinAvail).setScale(8, RoundingMode.FLOOR);
                }
                
                double maxCoinWithdrawQuantity = maximumCurrencyQuantity/coinPrice.doubleValue();
                
                //don't withdraw more than the max configured
                if(coinQuantityToWithdraw.doubleValue() > maxCoinWithdrawQuantity)
                {
                    coinQuantityToWithdraw = new BigDecimal(maxCoinWithdrawQuantity).setScale(8, RoundingMode.FLOOR);
                }

                //CryptomoneyAutotask.logMultiplexer.LogMessage("DEBUG: searching coinbase accounts for "+this.coinCurrencyType.toString());
                String coinCoinbaseAccount_Id = this.account.getCoinbaseRegularAccount_Id(WalletAccountCurrency.valueOf(this.coinCurrencyType.toString()));

                if(coinCoinbaseAccount_Id == null) 
                {
                    CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR coinbase wallet "+this.coinCurrencyType.toString()+" account not found");
                    System.exit(1);
                }

                //BigDecimal bdBTCAmountToWithdraw = BigDecimal.valueOf(btcToWithdraw).setScale(8, RoundingMode.HALF_EVEN);
                BigDecimal estimatedFiatAmountOfWithdrawal = coinQuantityToWithdraw.multiply(coinPrice).setScale(2, RoundingMode.UP);
                
                PaymentResponse response = CryptomoneyAutotask.withdrawalsService.makeWithdrawalToCoinbase(coinQuantityToWithdraw, this.coinCurrencyType.toString(), coinCoinbaseAccount_Id); //API CALL

                String logString = "Requested "+this.coinCurrencyType.toString()+" withdrawal to coinbase, response: " + response.getId() + " " + response.getCurrency() + " " + response.getAmount().doubleValue() + " " + response.getPayout_at() + " estimated "+this.fiatCurrencyType.toString()+" value: " + estimatedFiatAmountOfWithdrawal;
                CryptomoneyAutotask.logMultiplexer.LogMessage(logString);


                getAssociatedAllowance().addToAllowance(estimatedFiatAmountOfWithdrawal.negate());

                //purge any extra allowance
                if(getAssociatedAllowance().getAllowance().doubleValue() > 0)
                {
                    CryptomoneyAutotask.logMultiplexer.LogMessage("purging "+this.coinCurrencyType.toString()+" withdraw allowance " + getAssociatedAllowance().getAllowance());
                    getAssociatedAllowance().resetAllowance();
                }
            }
            else
            {
                CryptomoneyAutotask.logProv.LogMessage("fiatBalanceValueOfCoinInCoinbasePRO less than minimumCurrencyQuantityThreshold");
            }
        }
        else
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage(this.coinCurrencyType.toString()+" available for withdraw is 0");
        }
        
        CryptomoneyAutotask.logProv.LogMessage("");
    }
    
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType() 
            + " coinCurrencyType:"+ this.coinCurrencyType
            + " fiatCurrencyType:"+ this.fiatCurrencyType                
            + " execsPerDay:" + maximumAvgOccurrencesPerDay
            + " minFiat:" + minimumFiatQuantityThreshold
            + " maxFiat:" + maximumCurrencyQuantity
                ;
    }
}

