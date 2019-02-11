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

import cryptomoney.autotask.currency.*;
import java.util.UUID;


/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class AllowanceFiat extends Allowance
{
    FiatCurrencyType fiatCurrencyType;
    UUID uuid;
    
    public AllowanceFiat(AllowanceType _allowanceType, FiatCurrencyType _fiatCurrencyType, UUID _uuid)
    {
        super(_allowanceType);
        fiatCurrencyType = _fiatCurrencyType;
        uuid = _uuid;
    }
    
    public FiatCurrencyType getFiatCurrencyType()
    {
        return this.fiatCurrencyType;
    }
    
    public UUID getUUID()
    {
        return this.uuid;
    }
}
