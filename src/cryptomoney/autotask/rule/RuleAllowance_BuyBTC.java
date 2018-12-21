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
public class RuleAllowance_BuyBTC extends Rule
{
    private double amountPerDayUSD;
    
    public RuleAllowance_BuyBTC()
    {
        super(RuleType.ALLOWANCE, ActionType.ALLOWANCE_BUY_BTC);
    }
    
    public RuleAllowance_BuyBTC(boolean _executeImmediately, double _amountPerDayUSD)
    {
        super(RuleType.ALLOWANCE, ActionType.ALLOWANCE_BUY_BTC);
        
        amountPerDayUSD = _amountPerDayUSD;
        
        if(_executeImmediately)
        {
            this.account.addAllowanceBuyBTCinUSD(amountPerDayUSD); //A FULL DAY'S AMOUNT
        }
    }
    
    @Override
    public void doAction()
    {
        CryptomoneyAutotask.logProv.LogMessage(getHelpString());
        
        int msPerDay = 1000*60*60*24;
        double intervalsPerDay =  msPerDay / CryptomoneyAutotask.iterationIntervalMS;
        
        double amountPerIntervalUSD = amountPerDayUSD / intervalsPerDay;
        CryptomoneyAutotask.logProv.LogMessage("actiontype: " + getActionType().toString() + " amount/interval: " + amountPerIntervalUSD);      
        
        this.account.addAllowanceBuyBTCinUSD(amountPerIntervalUSD);
        CryptomoneyAutotask.logProv.LogMessage("new allowanceBuyBTCinUSD: " + account.getAllowanceBuyBTCinUSD());     
        
        CryptomoneyAutotask.logProv.LogMessage("");
    }
    
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType() + "";
    }
}
