// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract SimpleERC20 {

    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);

    mapping(address => uint256) private _balances;
    mapping(address => mapping (address => uint256)) private _approvals;
    address private _ownerAddress;

    constructor(address[] memory accounts, uint256[] memory balances, address ownerAddress) {
        require(accounts.length == balances.length, "Mismatching account and balance array lengths");

        for (uint i = 0; i < accounts.length; i++) {
            _balances[accounts[i]] = balances[i];
        }

        _ownerAddress = ownerAddress;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function approve(address spender, uint256 amount) public {
        _approvals[spender][msg.sender] = amount;
        emit Approval(msg.sender, spender, amount);
    }

    function transfer(address recipient, uint256 amount) public returns (bool) {
        require(recipient != address(0), "Transfer to the zero address");
        require(_balances[msg.sender] >= amount, "Transfer amount exceeds balance");

        _balances[msg.sender] -= amount;
        _balances[recipient] += amount;

        emit Transfer(msg.sender, recipient, amount);

        return true;
    }

    function transferFrom(address from, address to, uint256 amount) public returns (bool) {
        require(from != address(0), "Transfer from the zero address");
        require(to != address(0), "Transfer to the zero address");
        require(_balances[from] >= amount, "Transfer amount exceeds balance");
        require(_approvals[msg.sender][from] >= amount, "Not enough approved balance");

        _balances[from] -= amount;
        _balances[to] += amount;
        _approvals[msg.sender][from] -= amount;

        emit Transfer(from, to, amount);

        return true;
    }
}
