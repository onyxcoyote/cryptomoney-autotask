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
public class CoinPriceLimits
{
    
    private ArrayList<CoinPriceLimit> limits = new ArrayList<>();
    
    public void addLimit(CoinPriceLimit limit)
    {
        limits.add(limit);
    }
    
    public CoinPriceLimit getLimit(CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType) throws Exception
    {
        for(CoinPriceLimit limit : limits)
        {
            if(limit.coinCurrencyType.equals(_coinCurrencyType) &&
                    limit.fiatCurrencyTypeForMeasure.equals(_fiatCurrencyType))
            {
                return limit;
            }
        }
        
        throw new Exception("Price limit not found " + _coinCurrencyType + " " + _fiatCurrencyType);
    }
}
