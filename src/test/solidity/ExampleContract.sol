// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract ExampleContract {

    struct ExampleStruct {
        address owner;
    }

    event ExampleEvent(ExampleStruct nonIndexedStruct, ExampleStruct indexed indexedStruct);

    address private _owner;

    constructor(address owner) payable {
        _owner = owner;
        emit ExampleEvent(ExampleStruct(owner), ExampleStruct(owner));
    }

    function setOwner(address owner) public {
        _owner = owner;
        emit ExampleEvent(ExampleStruct(owner), ExampleStruct(owner));
    }

    function getOwner() public view returns (address) {
        return _owner;
    }
}
