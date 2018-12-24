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
import java.math.BigDecimal;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAllowance_DepositUSD extends Rule
{
    private double amountPerDayUSD;
    
    public RuleAllowance_DepositUSD()
    {
        super(RuleType.ALLOWANCE, ActionType.ALLOWANCE_DEPOSIT_USD);
    }
    
    public RuleAllowance_DepositUSD(boolean _executeImmediately, double _amountPerDayUSD)
    {
        super(RuleType.ALLOWANCE, ActionType.ALLOWANCE_DEPOSIT_USD);
        amountPerDayUSD = _amountPerDayUSD;
        
        if(_executeImmediately)
        {
            this.account.allowanceDepositUSD.addToAllowance(BigDecimal.valueOf(amountPerDayUSD)); //A FULL DAY'S AMOUNT
        }
        
        CryptomoneyAutotask.logProv.LogMessage("CREATED actiontype: " + getActionType().toString() + " currentAmount: " + this.account.allowanceDepositUSD.getAllowance().doubleValue());
    }
    
    @Override
    public void doAction()
    {
        //CryptomoneyAutotask.logProv.LogMessage(getHelpString());
        
        int msPerDay = 1000*60*60*24;
        double intervalsPerDay =  msPerDay / CryptomoneyAutotask.iterationIntervalMS;
        
        double amountPerIntervalUSD = amountPerDayUSD / intervalsPerDay;
        
        
        this.account.allowanceDepositUSD.addToAllowance(BigDecimal.valueOf(amountPerIntervalUSD));
        CryptomoneyAutotask.logProv.LogMessage("STATUS actiontype: " + getActionType().toString() + "new AllowanceDepositUSD: " + account.allowanceDepositUSD.getAllowance());
     
        //CryptomoneyAutotask.logProv.LogMessage("");
    }
    
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType()                 
                +  " amountPerDayUSD:" + amountPerDayUSD;
    }
}
