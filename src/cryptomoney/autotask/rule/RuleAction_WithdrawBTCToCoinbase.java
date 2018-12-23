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
    private double maximumAvgOccurrencesPerDay;
    private double minimumUSDQuantityThreshold;
    private double maximumUSDQuantity;
    
    private int executionCount = 0; //if we set this to 999999 then it would execute right away upon running program (maybe)
    
    public RuleAction_WithdrawBTCToCoinbase()
    {
        super(RuleType.ACTION, ActionType.ACTION_WITHDRAW_BTC_TO_COINBASE);
    }
    
    public RuleAction_WithdrawBTCToCoinbase(boolean _executeImmediately, double _maximumAvgOccurrencesPerDay, double _minimumUSDQuantityThreshold, double _maximumUSDQuantity)
    {
        super(RuleType.ACTION, ActionType.ACTION_WITHDRAW_BTC_TO_COINBASE);

        maximumAvgOccurrencesPerDay = _maximumAvgOccurrencesPerDay;
        minimumUSDQuantityThreshold = _minimumUSDQuantityThreshold;
        maximumUSDQuantity = _maximumUSDQuantity;
        
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
    
    @Override
    public void doAction()
    {
        //CryptomoneyAutotask.logProv.LogMessage(getHelpString());
        
        executionCount++;

        
        
        if(this.account.allowanceWithdrawBTCToCoinbaseInUSD.getAllowance().doubleValue() < minimumUSDQuantityThreshold)
        {
            CryptomoneyAutotask.logProv.LogMessage(getHelpString() + " account.getAllowanceWithdrawBTCToCoinbaseInUSD() does not exceed minimumUSDQuantityThreshold " + this.account.allowanceWithdrawBTCToCoinbaseInUSD.getAllowance() + "/" + minimumUSDQuantityThreshold);
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
        }
        
        
        
        BigDecimal btcPrice = SharedFunctions.GetBestBTCBuyPrice();
        
        //todo: only get the specific account needed
        List<Account> accounts = CryptomoneyAutotask.accountService.getAccounts();
        CryptomoneyAutotask.logProv.LogMessage("retrieved coinbase PRO accounts, count: "+accounts.size());
        
        Account btcAccount = null;
        for(Account acct : accounts)
        {
            CryptomoneyAutotask.logProv.LogMessage("CPB account retrieved: " + acct.getId() + " " + acct.getCurrency() + " " + acct.getAvailable() + "/" + acct.getBalance());
            if(acct.getCurrency().equals("BTC"))
            {
                if(btcAccount != null)
                {
                    CryptomoneyAutotask.logProv.LogMessage("ERROR, TWO BTC ACCOUNTS FOUND WHEN EXPECTINE ONE, EXITING"); //todo: test this to make sure there would only be one account
                    System.exit(1);
                }
                btcAccount = acct;
            }
        }
        
        double usdBalanceValueOfBTCInCoinbasePRO = 0;
        BigDecimal btcAvail = btcAccount.getAvailable();
        double valBtcAvail = 0;
        
        if(btcAvail != null)
        { 
            valBtcAvail = btcAvail.doubleValue();
            usdBalanceValueOfBTCInCoinbasePRO = valBtcAvail*btcPrice.doubleValue();
        }
        
        if(valBtcAvail > 0)
        {
            if(usdBalanceValueOfBTCInCoinbasePRO >= minimumUSDQuantityThreshold)

            CryptomoneyAutotask.logProv.LogMessage("actiontype: " + getActionType().toString());

            double btcToWithdraw = valBtcAvail;
            if(btcToWithdraw > maximumUSDQuantity)
            {
                btcToWithdraw = maximumUSDQuantity;
            }

            List<CoinbaseAccount> coinbaseAccounts = CryptomoneyAutotask.paymentService.getCoinbaseAccounts(); //optional: instead of this get the id somehow else and code it into config?
            CryptomoneyAutotask.logProv.LogMessage("retrieved coinbase accounts, count: "+coinbaseAccounts.size());
            
            CoinbaseAccount btcCoinbaseAccount = null;
            for(CoinbaseAccount coinbaseAccount : coinbaseAccounts)
            {
                CryptomoneyAutotask.logProv.LogMessage("coinbase account retrieved: " + coinbaseAccount.getId() + " " + 
                        coinbaseAccount.getCurrency() + " " + 
                        coinbaseAccount.getType() + " " + 
                        coinbaseAccount.getPrimary() + " " + 
                        coinbaseAccount.getBalance() + " " + 
                        coinbaseAccount.getName()
                        );
                if(coinbaseAccount.getCurrency().equals("BTC") && coinbaseAccount.getPrimary())
                {
                    if(btcCoinbaseAccount != null)
                    {
                        CryptomoneyAutotask.logProv.LogMessage("ERROR, TWO -PRIMARY- BTC ACCOUNTS FOUND WHEN EXPECTINE ONE, EXITING"); //todo: test this to make sure there would only be one account
                            //in this case, I think there COULD be multiple accounts unless the getPrimary() takes care of it
                        System.exit(1);
                    }
                    btcCoinbaseAccount = coinbaseAccount;
                }
            }
            
            if(btcCoinbaseAccount == null)
            {
                CryptomoneyAutotask.logProv.LogMessage("ERROR BTC coinbase account not found");
                System.exit(1);
            }
            
            BigDecimal bdBTCAmountToWithdraw = BigDecimal.valueOf(btcToWithdraw).setScale(8, RoundingMode.HALF_EVEN);
            
            PaymentResponse response = CryptomoneyAutotask.withdrawalsService.makeWithdrawalToCoinbase(bdBTCAmountToWithdraw, "BTC", btcCoinbaseAccount.getId()); //API CALL
            
            String logString = "Requested BTC withdrawal, response: " + response.getId() + " " + response.getCurrency() + " " + response.getAmount().doubleValue() + " " + response.getPayout_at();
            CryptomoneyAutotask.logProv.LogMessage(logString);
            CryptomoneyAutotask.logProvFile.LogMessage(logString);
            
            BigDecimal estimatedAmountOfWithDrawal = bdBTCAmountToWithdraw.multiply(btcPrice).setScale(2, RoundingMode.HALF_EVEN);
            this.account.allowanceWithdrawBTCToCoinbaseInUSD.addToAllowance(estimatedAmountOfWithDrawal.negate());
                    
            //purge any extra allowance
            if(this.account.allowanceWithdrawBTCToCoinbaseInUSD.getAllowance().doubleValue() > 0)
            {
                CryptomoneyAutotask.logProv.LogMessage("purging BTC withdraw allowance " + this.account.allowanceWithdrawBTCToCoinbaseInUSD.getAllowance());
                CryptomoneyAutotask.logProvFile.LogMessage("purging BTC withdraw allowance " + this.account.allowanceWithdrawBTCToCoinbaseInUSD.getAllowance());
                this.account.allowanceWithdrawBTCToCoinbaseInUSD.resetAllowance();
            }
                    
        }
        else
        {
            CryptomoneyAutotask.logProv.LogMessage("BTC available for withdraw is 0");
            CryptomoneyAutotask.logProvFile.LogMessage("BTC available for withdraw is 0");
        }
        
        CryptomoneyAutotask.logProv.LogMessage("");
    }
    
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType() + "";
    }
}

