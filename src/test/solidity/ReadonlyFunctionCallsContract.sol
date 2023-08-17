// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract ReadonlyFunctionCallsContract {
    function returningUint(uint256 input) public pure returns (uint256) {
        return input;
    }
}
