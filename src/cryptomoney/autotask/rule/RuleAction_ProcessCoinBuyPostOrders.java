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
import cryptomoney.autotask.CryptomoneyAutotask;
import cryptomoney.autotask.currency.CoinCurrencyType;
import cryptomoney.autotask.currency.FiatCurrencyType;
import cryptomoney.autotask.functions.SharedFunctions;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAction_ProcessCoinBuyPostOrders extends Rule
{
    private CoinCurrencyType coinCurrencyType;
    private FiatCurrencyType fiatCurrencyType;
    private double maximumAvgOccurrencesPerDay;
    
    private int executionCount = 0; //if we set this to 999999 then it would execute right away upon running program (maybe)

    
    public RuleAction_ProcessCoinBuyPostOrders()
    {
        super(RuleType.ACTION, ActionType.ACTION_PROCESS_COIN_BUY_POST_ORDERS);
    }
    
    /**
     * 
     * @param _maximumAvgOccurrencesPerDay
     */
    public RuleAction_ProcessCoinBuyPostOrders(CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType, boolean _executeImmediately, double _maximumAvgOccurrencesPerDay)
    {
        super(RuleType.ACTION, ActionType.ACTION_PROCESS_COIN_BUY_POST_ORDERS);
        coinCurrencyType = _coinCurrencyType;
        fiatCurrencyType = _fiatCurrencyType;
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
        
        if(this.account.getKnownPendingOrderCount() == 0)
        {
            CryptomoneyAutotask.logProv.LogMessage(getHelpString() + " no orders to process");
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
        
        this.account.ProcessBuyOrders(this.coinCurrencyType, this.fiatCurrencyType, false);
        
        CryptomoneyAutotask.logProv.LogMessage("");
    }
            
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType() 
            + " coinCurrencyType:"+ this.coinCurrencyType
            + " fiatCurrencyType:"+ this.fiatCurrencyType                
                + " execsPerDay:" + maximumAvgOccurrencesPerDay;
    }
}
