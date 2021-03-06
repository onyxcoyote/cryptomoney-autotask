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

import cryptomoney.autotask.exchangeaccount.ExchangeAccount;
import cryptomoney.autotask.CryptomoneyAutotask;
import cryptomoney.autotask.currency.CoinCurrencyType;
import cryptomoney.autotask.currency.FiatCurrencyType;

import java.util.UUID;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public abstract class Rule
{
    UUID uuid;
    ExchangeAccount account;
    private ActionType actionType;
    private RuleType ruleType;
    CoinCurrencyType coinCurrencyType;
    FiatCurrencyType fiatCurrencyType;    
    
    public Rule()
    {
        
    }
    
    public Rule(UUID _uuid, RuleType _ruleType, ActionType _actionType)
    {
        uuid = _uuid;
        account = CryptomoneyAutotask.app.account1; //there is only one of these... TODO
        ruleType = _ruleType;
        actionType = _actionType;
        //System.out.println("TEMP constructor: " + actionType.toString());
    }

    public UUID getUUID()
    {
        return uuid;
    }
    
    /**
     * @return the ruleType
     */
    public RuleType getRuleType()
    {
        return ruleType;
    }
    
    /**
     * @return the actionType
     */
    public ActionType getActionType()
    {
        return actionType;
    }
    
    public CoinCurrencyType getCoinCurrencyType()
    {
        return this.coinCurrencyType;
    }

    public FiatCurrencyType getFiatCurrencyType()
    {
        return this.fiatCurrencyType;
    }    
    
    public abstract void doAction();

    public abstract String getHelpString();

}
