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

import cryptomoney.autotask.currency.FiatCurrencyType;
import cryptomoney.autotask.currency.CoinCurrencyType;
import java.util.ArrayList;


/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class Rules extends ArrayList<Rule>
{
    public Rule getRule(RuleType _ruleType, ActionType _actionType)
    {
        for(Rule rule : this)
        {
            if(rule.getRuleType().equals(_ruleType) &&
                    rule.getActionType().equals(_actionType))
            {
                return rule;
            }
        }
        
        return null;
    }
    
    public Rule getRule(RuleType _ruleType, ActionType _actionType, CoinCurrencyType _coinCurrencyType)
    {
        for(Rule rule : this)
        {
            if(rule.getRuleType().equals(_ruleType) &&
                    rule.getActionType().equals(_actionType) &&
                    rule.getCoinCurrencyType().equals(_coinCurrencyType))
            {
                return rule;
            }
        }
        
        return null;
    }
    
    public Rule getRule(RuleType _ruleType, ActionType _actionType, FiatCurrencyType _fiatCurrencyType)
    {
        for(Rule rule : this)
        {
            if(rule.getRuleType().equals(_ruleType) &&
                    rule.getActionType().equals(_actionType) &&
                    rule.getFiatCurrencyType().equals(_fiatCurrencyType))
            {
                return rule;
            }
        }
        
        return null;
    }   
    
    public Rule getRule(RuleType _ruleType, ActionType _actionType, FiatCurrencyType _fiatCurrencyType, CoinCurrencyType _coinCurrencyType)
    {
        for(Rule rule : this)
        {
            if(rule.getRuleType().equals(_ruleType) &&
                    rule.getActionType().equals(_actionType) &&
                    rule.getFiatCurrencyType().equals(_fiatCurrencyType) &&
                    rule.getCoinCurrencyType().equals(_coinCurrencyType)
                    )
            {
                return rule;
            }
        }
        
        return null;
    }        
}
