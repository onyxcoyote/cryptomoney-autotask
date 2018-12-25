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
import cryptomoney.autotask.allowance.AllowanceCoinFiat;
import cryptomoney.autotask.allowance.AllowanceType;
import cryptomoney.autotask.currency.CoinCurrencyType;
import cryptomoney.autotask.currency.FiatCurrencyType;
import java.math.BigDecimal;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAllowance_BuyCoin extends Rule
{
    private CoinCurrencyType coinCurrencyType;
    private FiatCurrencyType fiatCurrencyType;    
    private double amountPerDayUSD;
    
    public RuleAllowance_BuyCoin()
    {
        super(RuleType.ALLOWANCE, ActionType.ALLOWANCE_BUY_BTC);
    }
    
    public RuleAllowance_BuyCoin(CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType, boolean _executeImmediately, double _amountPerDayUSD)
    {
        super(RuleType.ALLOWANCE, ActionType.ALLOWANCE_BUY_BTC);
        coinCurrencyType = _coinCurrencyType;
        fiatCurrencyType = _fiatCurrencyType;
        amountPerDayUSD = _amountPerDayUSD;
        
        if(_executeImmediately)
        {
            getAssociatedAllowance().addToAllowance(BigDecimal.valueOf(amountPerDayUSD)); //A FULL DAY'S AMOUNT
        }
        
        CryptomoneyAutotask.logProv.LogMessage("CREATED actiontype: " + getActionType().toString() + " current amount: " + getAssociatedAllowance().getAllowance().doubleValue());      
    }
    
    private AllowanceCoinFiat getAssociatedAllowance()
    {
        return this.account.getAllowanceCoinFiat(AllowanceType.Buy, coinCurrencyType, fiatCurrencyType);
    }
    
    @Override
    public void doAction()
    {
        //CryptomoneyAutotask.logProv.LogMessage(getHelpString());
        
        int msPerDay = 1000*60*60*24;
        double intervalsPerDay =  msPerDay / CryptomoneyAutotask.iterationIntervalMS;
        
        double amountPerIntervalUSD = amountPerDayUSD / intervalsPerDay;
        
        getAssociatedAllowance().addToAllowance(BigDecimal.valueOf(amountPerIntervalUSD));
        CryptomoneyAutotask.logProv.LogMessage("STATUS actiontype: " + getActionType().toString() + " new allowanceBuyBTCinUSD: " + getAssociatedAllowance().getAllowance());     
        
        //CryptomoneyAutotask.logProv.LogMessage("");
    }
    
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType() 
                +  " amountPerDayUSD:" + amountPerDayUSD;
    }
}
