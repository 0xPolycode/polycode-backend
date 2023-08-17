// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract EventEmitter {

    event ExampleEvent(address indexed owner, string indexed message, uint amount, uint256 indexed value, bool example);
    event AnonymousEvent(address indexed owner, string message) anonymous;

    constructor(address owner, string memory message, uint256 amount, uint256 value, bool example) {
        emit ExampleEvent(owner, message, amount, value, example);
        emit AnonymousEvent(owner, message);
    }

    function emitEvents(
        address owner,
        string memory message,
        uint256 amount,
        uint256 value,
        bool example
    ) public returns (bool) {
        emit ExampleEvent(owner, message, amount, value, example);
        emit AnonymousEvent(owner, message);
        return true;
    }
}
