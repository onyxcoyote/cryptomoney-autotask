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

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAllowance_DepositUSD extends Rule
{
    private double amountPerDayUSD;
    
    public RuleAllowance_DepositUSD()
    {
    }
    
    public RuleAllowance_DepositUSD(double _amountPerDayUSD)
    {
        super(RuleType.ALLOWANCE, ActionType.ALLOWANCE_DEPOSIT_USD);
        amountPerDayUSD = _amountPerDayUSD;
    }
    
    @Override
    public void doAction()
    {
        
        int msPerDay = 1000*60*60*24;
        double intervalsPerDay =  msPerDay / CryptomoneyAutotask.iterationIntervalMS;
        
        double amountPerIntervalUSD = amountPerDayUSD / intervalsPerDay;
        CryptomoneyAutotask.logProv.LogMessage("actiontype: " + getActionType().toString() + " amount/interval: " + amountPerIntervalUSD);      
        
        this.account.addAllowanceDepositUSD(amountPerIntervalUSD);
        CryptomoneyAutotask.logProv.LogMessage("new AllowanceDepositUSD: " + account.getAllowanceDepositUSD());
        
    }
    
    @Override
    public String getHelpString()
    {
        zz
    }
}
