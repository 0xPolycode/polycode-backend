// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract SimpleLockManager {

    bool private _locked;

    function lock(
        address tokenAddress,
        uint256 amount,
        uint256 duration,
        string memory info,
        address unlockPrivilegeWallet
    ) public {
        _locked = true;
    }

    function isLocked() public view returns (bool) {
        return _locked;
    }
}
