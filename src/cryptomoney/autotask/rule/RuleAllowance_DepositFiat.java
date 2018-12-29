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
import cryptomoney.autotask.allowance.AllowanceFiat;
import cryptomoney.autotask.allowance.AllowanceType;
import cryptomoney.autotask.currency.FiatCurrencyType;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAllowance_DepositFiat extends Rule
{
    private FiatCurrencyType fiatCurrencyType;    
    private double amountPerDayFiat;
    
    public RuleAllowance_DepositFiat()
    {
        super(RuleType.ALLOWANCE, ActionType.ALLOWANCE_DEPOSIT_FIAT);
    }
    
    public RuleAllowance_DepositFiat(FiatCurrencyType _fiatCurrencyType, boolean _executeImmediately, double _amountPerDayFiat)
    {
        super(RuleType.ALLOWANCE, ActionType.ALLOWANCE_DEPOSIT_FIAT);
        fiatCurrencyType = _fiatCurrencyType;
        amountPerDayFiat = _amountPerDayFiat;
        
        if(_executeImmediately)
        {
            getAssociatedAllowance().addToAllowance(BigDecimal.valueOf(amountPerDayFiat)); //A FULL DAY'S AMOUNT
        }
        
        CryptomoneyAutotask.logProv.LogMessage("CREATED actiontype: " + getActionType().toString() + " currentAmount: " + getAssociatedAllowance().getAllowance().doubleValue());
    }
    
    private AllowanceFiat getAssociatedAllowance()
    {
        return this.account.getAllowanceFiat(AllowanceType.Deposit, fiatCurrencyType);
    }
    
    @Override
    public void doAction()
    {
        //CryptomoneyAutotask.logProv.LogMessage(getHelpString());
        
        int msPerDay = 1000*60*60*24;
        double intervalsPerDay =  msPerDay / CryptomoneyAutotask.iterationIntervalMS;
        
        double amountPerIntervalFiat = amountPerDayFiat / intervalsPerDay;
        
        
        getAssociatedAllowance().addToAllowance(BigDecimal.valueOf(amountPerIntervalFiat));
        CryptomoneyAutotask.logProv.LogMessage("STATUS actiontype: " + getActionType().toString() + "new AllowanceDepositFiat: " + getAssociatedAllowance().getAllowance().setScale(2, RoundingMode.FLOOR));
     
        //CryptomoneyAutotask.logProv.LogMessage("");
    }
    
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType()                 
            + " fiatCurrencyType:"+ this.fiatCurrencyType
                +  " amountPerDayFiat:" + amountPerDayFiat;
    }
}
