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
package cryptomoney.autotask.exchangeaccount;

import java.util.ArrayList;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class WalletAccountIDs
{
    private ArrayList<WalletAccountID> accounts = new ArrayList<>();
    
    public WalletAccountIDs()
    {
    }
    
    
    public WalletAccountID getAccountID(WalletAccountType _walletAccountType, WalletAccountCurrency _walletAccountCurrency, boolean _getOnly)
    {
        for(WalletAccountID walletAccountID : accounts)
        {
            if(walletAccountID.getWalletAccountType().equals(_walletAccountType) &&
                    walletAccountID.getWalletAccountCurrency().equals(_walletAccountCurrency))
            {
                return walletAccountID;
            }
        }
        
        
        if(!_getOnly)
        {
            WalletAccountID walletAccountID = new WalletAccountID(_walletAccountType, _walletAccountCurrency, null);
            this.accounts.add(walletAccountID);
            return walletAccountID;
        }
        else
        {
            return null; //does not exist yet, don't insert it, we're just checking.  return null to show it does not exist.
        }
        
        
    }
}
