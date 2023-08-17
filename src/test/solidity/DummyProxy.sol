// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract DummyProxy {

    address private _implementation;

    constructor(address impl) payable {
        _implementation = impl;
    }

    function implementation() public view returns (address) {
        return _implementation;
    }
}
