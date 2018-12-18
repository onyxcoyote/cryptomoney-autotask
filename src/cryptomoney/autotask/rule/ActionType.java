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

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public enum ActionType
{
    ALLOWANCE_BUY_BTC_POSTONLY,
    ALLOWANCE_WITHDRAW_BTC,
    ALLOWANCE_DEPOSIT_USD,
    
    ACTION_BUY_BTC_DCA,
    ACTION_WITHDRAW_BTC,
    ACTION_DEPOSIT_USD,
    
    ALARM_LOW_USD,
    ALARM_LOW_BTC,
    ALARM_ALL_BALANCES
}