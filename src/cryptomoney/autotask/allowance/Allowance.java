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
package cryptomoney.autotask.allowance;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class Allowance
{
    private BigDecimal allowanceAmount = new BigDecimal(0.0).setScale(8, RoundingMode.HALF_EVEN);
    
    public Allowance()
    {
        System.err.println("DEBUG: starting value " + allowanceAmount);
    }
    
    public BigDecimal getAllowance()
    {
        return allowanceAmount; 
    }
    
    public void resetAllowance()
    {
        allowanceAmount = new BigDecimal(0.0).setScale(8, RoundingMode.HALF_EVEN);
    }
    
    public void addToAllowance(BigDecimal _amountToAdd)
    {
        allowanceAmount = allowanceAmount.add(_amountToAdd);
    }
         
}
