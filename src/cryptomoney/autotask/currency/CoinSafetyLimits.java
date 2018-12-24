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
package cryptomoney.autotask.currency;

import java.util.ArrayList;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class CoinSafetyLimits
{
    
    private ArrayList<CoinSafetyLimit> limits;
    
    public void addLimit(CoinSafetyLimit limit)
    {
        limits.add(limit);
    }
    
    public double getLimit(CoinCurrencyType _coinCurrencyType) throws Exception
    {
        for(CoinSafetyLimit limit : limits)
        {
            if(limit.coinCurrencyType.equals(_coinCurrencyType))
            {
                return limit.maximumAmount;
            }
        }
        
        throw new Exception("Safety limit not found " + _coinCurrencyType);
    }
            
}
