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
            
            for(int i=0;i<100 && executionCount > numberOfExecutionsBeforeExecutingOnce;i++)
            {
                if(executionCount > numberOfExecutionsBeforeExecutingOnce)
                {
                    executionCount-=numberOfExecutionsBeforeExecutingOnce; //don't let it run a bunch of times in a row
                }
            }
            
        }
        
        
        if(this.account.isHas_coinbaseProBTCAccountId())
        {
            Account acct = this.account.getCoinbaseProBTCAccount();
            CryptomoneyAutotask.logMultiplexer.LogMessage("Coinbase PRO " + acct.getCurrency() + " " + acct.getAvailable() + " " + acct.getBalance());
        }
        
        if(this.account.isHas_coinbaseProUSDAccountId())
        {
            Account acct = this.account.getCoinbaseProUSDAccount();
            CryptomoneyAutotask.logMultiplexer.LogMessage("Coinbase PRO " + acct.getCurrency() + " " + acct.getAvailable() + " " + acct.getBalance());
        }
        
        /*if(this.account.isHas_coinbaseProUSDBankPaymentTypeId())
        {
            //nothing to display
            //String acct_id = this.account.getCoinbaseProUSDBankPaymentType_Id();
            //CryptomoneyAutotask.logMultiplexer.LogMessage(acct.getCurrency() + " " + acct.getAvailable() + " " + acct.getBalance());
        }*/
        
        if(this.account.isHas_coinbaseRegularBTCAccountId())
        {
            String account_id = this.account.getCoinbaseRegularBTCAccount_Id();
            CoinbaseAccount acct = this.account.getCoinbaseRegularBTCAccountById(account_id);
            CryptomoneyAutotask.logMultiplexer.LogMessage("Coinbase (regular) " + acct.getCurrency() + " " + acct.getBalance());
        }
        
        
        
        CryptomoneyAutotask.logProv.LogMessage("");
    }
            
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType() 
                + " maximumAvgOccurrencesPerDay:" + maximumAvgOccurrencesPerDay;
    }
}
