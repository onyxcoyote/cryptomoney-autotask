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
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAction_WithdrawBTCToCoinbase extends Rule
{
    private double amountPerDayUSD;
    private double minimumUSDQuantityThreshold;
    private double maximumUSDQuantity;
    
    public RuleAction_WithdrawBTCToCoinbase()
    {
    }
    
    public RuleAction_WithdrawBTCToCoinbase(double _amountPerDayUSD, double _minimumUSDQuantityThreshold, double _maximumUSDQuantity)
    {
        super(RuleType.ACTION, ActionType.ACTION_WITHDRAW_BTC_TO_COINBASE);
        amountPerDayUSD = _amountPerDayUSD;
        minimumUSDQuantityThreshold = _minimumUSDQuantityThreshold;
        maximumUSDQuantity = _maximumUSDQuantity;
    }
    
    @Override
    public void doAction()
    {
        //int msPerDay = 1000*60*60*24;
        //double intervalsPerDay =  msPerDay / cbpdca.Cbpdca.iterationIntervalMS;
        //double amountPerInterval = amountPerDayUSD / intervalsPerDay;
        
        
        if(this.account.getAllowanceWithdrawBTCToCoinbaseInUSD() < minimumUSDQuantityThreshold)
        {
            CryptomoneyAutotask.logProv.LogMessage("account.getAllowanceWithdrawBTCToCoinbaseInUSD() does not exceed minimumUSDQuantityThreshold");
            return;
        }
        
        //TODO: RANDOM CHANCE TO PROCEED
        //todo: add regular delay as well?
        
        int btcPrice = 3100; //TODO: FIX THIS!
        
        //TODO: we need a regular delay to prevent this from happening too much
        List<Account> accounts = CryptomoneyAutotask.accountService.getAccounts();
        CryptomoneyAutotask.logProv.LogMessage("retrieved coinbase PRO accounts, count: "+accounts.size());
        
        Account btcAccount = null;
        for(Account acct : accounts)
        {
            if(acct.getCurrency().equals("BTC"))
            {
                CryptomoneyAutotask.logProv.LogMessage("account retrieved: " + acct.toString());
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
            usdBalanceValueOfBTCInCoinbasePRO = valBtcAvail*btcPrice;
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


            List<CoinbaseAccount> coinbaseAccounts = CryptomoneyAutotask.paymentService.getCoinbaseAccounts(); //TODO: instead of this get the id somehow else and code it into config
            CryptomoneyAutotask.logProv.LogMessage("retrieved coinbase accounts, count: "+coinbaseAccounts.size());
            
            CoinbaseAccount btcCoinbaseAccount = null;
            for(CoinbaseAccount coinbaseAccount : coinbaseAccounts)
            {
                CryptomoneyAutotask.logProv.LogMessage("coinbase account retrieved: " + coinbaseAccount.toString());
                if(coinbaseAccount.getCurrency().equals("BTC") && coinbaseAccount.getPrimary())
                {
                    if(btcAccount != null)
                    {
                        CryptomoneyAutotask.logProv.LogMessage("ERROR, TWO -PRIMARY- BTC ACCOUNTS FOUND WHEN EXPECTINE ONE, EXITING"); //todo: test this to make sure there would only be one account
                        //TODO: in this case, I think there COULD be multiple accounts unless the getPrimary() takes care of it
                        System.exit(1);
                    }
                    btcCoinbaseAccount = coinbaseAccount;
                }
            }
            
            
            BigDecimal bdBTCAmountToWithdraw = BigDecimal.valueOf(btcToWithdraw).setScale(8, RoundingMode.HALF_EVEN);
            
            PaymentResponse response = CryptomoneyAutotask.withdrawalsService.makeWithdrawalToCoinbase(bdBTCAmountToWithdraw, "BTC", btcCoinbaseAccount.getId());
            CryptomoneyAutotask.logProv.LogMessage("USD deposit response: " + response.toString());
                    
                    
        }
        else
        {
            CryptomoneyAutotask.logProv.LogMessage("BTC available for withdraw is 0");
        }
        
    }
    
    @Override
    public String getHelpString()
    {
        zz
    }
}
