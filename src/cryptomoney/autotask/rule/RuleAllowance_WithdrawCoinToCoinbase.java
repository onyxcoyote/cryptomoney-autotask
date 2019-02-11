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

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAllowance_WithdrawCoinToCoinbase extends Rule
{
    private CoinCurrencyType coinCurrencyType;
    private FiatCurrencyType fiatCurrencyType;    
    private double amountPerDayFiat;
    
    public RuleAllowance_WithdrawCoinToCoinbase()
    {
        super(null, RuleType.ALLOWANCE, ActionType.ALLOWANCE_WITHDRAW_COIN_TO_COINBASE);
    }
    
    public RuleAllowance_WithdrawCoinToCoinbase(UUID _uuid, CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType, boolean _executeImmediately, double _amountPerDayFiat)
    {
        super(_uuid, RuleType.ALLOWANCE, ActionType.ALLOWANCE_WITHDRAW_COIN_TO_COINBASE);
        coinCurrencyType = _coinCurrencyType;
        fiatCurrencyType = _fiatCurrencyType;        
        amountPerDayFiat = _amountPerDayFiat;
        
        if(_executeImmediately)
        {
            getAssociatedAllowance().addToAllowance(BigDecimal.valueOf(amountPerDayFiat)); //A FULL DAY'S AMOUNT
        }
        
        CryptomoneyAutotask.logProv.LogMessage("CREATED actiontype: " + getActionType().toString() + " currentAmount: " + getAssociatedAllowance().getAllowance().doubleValue());
    }
    
    private AllowanceCoinFiat getAssociatedAllowance()
    {
        return this.account.getAllowanceCoinFiat(AllowanceType.WithdrawCoinToCoinbase, coinCurrencyType, fiatCurrencyType, this.uuid);
    }
    
    @Override
    public void doAction()
    {
        //CryptomoneyAutotask.logProv.LogMessage(getHelpString());
        
        int msPerDay = 1000*60*60*24;
        double intervalsPerDay =  msPerDay / CryptomoneyAutotask.iterationIntervalMS;
        
        double amountPerIntervalFiat = amountPerDayFiat / intervalsPerDay;
        //CryptomoneyAutotask.logProv.LogMessage("actiontype: " + getActionType().toString() + " amount/interval: " + amountPerIntervalUSD);      
        
        getAssociatedAllowance().addToAllowance(BigDecimal.valueOf(amountPerIntervalFiat));
        CryptomoneyAutotask.logProv.LogMessage("STATUS actiontype: " + getActionType().toString() + " new allowanceWithdrawCoinToCoinbaseInFiat: " + getAssociatedAllowance().getAllowance().setScale(2, RoundingMode.FLOOR));      
     
        //CryptomoneyAutotask.logProv.LogMessage("");
    }
    
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType()                 
            + " coinCurrencyType:"+ this.coinCurrencyType
            + " fiatCurrencyType:"+ this.fiatCurrencyType  
                +  " amountPerDayFiat:" + amountPerDayFiat;
    }
}
