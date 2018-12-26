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
import com.coinbase.exchange.api.orders.Order;
import com.coinbase.exchange.api.accounts.Account;
import com.coinbase.exchange.api.payments.CoinbaseAccount;
import com.coinbase.exchange.api.payments.PaymentType;

import cryptomoney.autotask.CryptomoneyAutotask;
import cryptomoney.autotask.exchangeaccount.*;
import cryptomoney.autotask.functions.SharedFunctions;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAlarm_PrintBalance extends Rule
{
    private double maximumAvgOccurrencesPerDay;
    
    private int executionCount = 0; //if we set this to 999999 then it would execute right away upon running program (maybe)

    
    public RuleAlarm_PrintBalance()
    {
        super(RuleType.ALARM, ActionType.ALARM_PRINT_BALANCE);
    }
    
    /**
     * 
     * @param _maximumAvgOccurrencesPerDay
     */
    public RuleAlarm_PrintBalance(boolean _executeImmediately, double _maximumAvgOccurrencesPerDay)
    {
        super(RuleType.ALARM, ActionType.ALARM_PRINT_BALANCE);
        maximumAvgOccurrencesPerDay = _maximumAvgOccurrencesPerDay;
        
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
        
        //enumerate possible options (even impossible ones, it will only show those have been loaded into memory)
        for(WalletAccountCurrency curr : WalletAccountCurrency.values())
        {
            //coinbase pro
            if(this.account.walletAccountIDs.getAccountID(WalletAccountType.CoinbaseProWallet, curr, true) != null)
            {
                Account acct = this.account.getCoinbaseProWalletAccount(curr);
                CryptomoneyAutotask.logMultiplexer.LogMessage("Coinbase PRO " + acct.getCurrency() + " " + acct.getAvailable() + " " + acct.getBalance());
            }
            
            //Coinbase regular
            if(this.account.walletAccountIDs.getAccountID(WalletAccountType.CoinbaseRegularWallet, curr, true) != null)
            {
                String account_id = this.account.getCoinbaseRegularAccount_Id(curr);
                CoinbaseAccount acct = this.account.getCoinbaseRegularAccount_ById(account_id);
                CryptomoneyAutotask.logMultiplexer.LogMessage("Coinbase (regular) " + acct.getCurrency() + " " + acct.getBalance());
            }
        }
        /*if(this.account.isHas_coinbaseProUSDBankPaymentTypeId()) //nothing to display, can't show balances, etc.
        {
            //String acct_id = this.account.getCoinbaseProUSDBankPaymentType_Id();
            //CryptomoneyAutotask.logMultiplexer.LogMessage(acct.getCurrency() + " " + acct.getAvailable() + " " + acct.getBalance());
        }*/
        
        CryptomoneyAutotask.logProv.LogMessage("");
    }
            
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType() 
                + " execsPerDay:" + maximumAvgOccurrencesPerDay;
    }
}
