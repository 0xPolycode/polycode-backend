// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./Structs.sol";

contract SimplePayoutManager {

    uint256 private currentPayoutId;
    mapping(uint256 => Structs.Payout) private payoutsById;
    mapping(address => uint256[]) private payoutsByAssetAddress;
    mapping(address => uint256[]) private payoutsByOwnerAddress;
    mapping(uint256 => mapping(address => uint256)) private payoutClaims;

    constructor(Structs.Payout[] memory _payouts) {
        for (uint i = 0; i < _payouts.length; i++) {
            Structs.Payout memory payout = _payouts[i];

            currentPayoutId = payout.payoutId + 1;
            payoutsById[payout.payoutId] = payout;
            payoutsByAssetAddress[payout.asset].push(payout.payoutId);
            payoutsByOwnerAddress[payout.payoutOwner].push(payout.payoutId);
        }
    }

    function setClaim(uint256 _payoutId, address _wallet, uint256 _amount) external {
        payoutClaims[_payoutId][_wallet] = _amount;
    }

    function getCurrentPayoutId() external view returns (uint256) {
        return currentPayoutId;
    }

    function getPayoutInfo(uint256 _payoutId) external view returns (Structs.Payout memory) {
        return payoutsById[_payoutId];
    }

    function getPayoutIdsForAsset(address _assetAddress) external view returns (uint256[] memory) {
        return payoutsByAssetAddress[_assetAddress];
    }

    function getPayoutsForAsset(address _assetAddress) external view returns (Structs.Payout[] memory) {
        uint256[] memory payoutIds = payoutsByAssetAddress[_assetAddress];
        Structs.Payout[] memory assetPayouts = new Structs.Payout[](payoutIds.length);

        for (uint i = 0; i < payoutIds.length; i++) {
            assetPayouts[i] = payoutsById[payoutIds[i]];
        }

        return assetPayouts;
    }

    function getPayoutIdsForOwner(address _ownerAddress) external view returns (uint256[] memory) {
        return payoutsByOwnerAddress[_ownerAddress];
    }

    function getPayoutsForOwner(address _ownerAddress) external view returns (Structs.Payout[] memory) {
        uint256[] memory payoutIds = payoutsByOwnerAddress[_ownerAddress];
        Structs.Payout[] memory ownerPayouts = new Structs.Payout[](payoutIds.length);

        for (uint i = 0; i < payoutIds.length; i++) {
            ownerPayouts[i] = payoutsById[payoutIds[i]];
        }

        return ownerPayouts;
    }

    function getAmountOfClaimedFunds(uint256 _payoutId, address _wallet) external view returns (uint256) {
        return payoutClaims[_payoutId][_wallet];
    }
}
