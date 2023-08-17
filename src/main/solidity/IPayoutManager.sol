// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./IERC20.sol";
import "./Structs.sol";

interface IPayoutManager {
    function getFeeManager() external view returns (address);
    function getCurrentPayoutId() external view returns (uint256);
    function getPayoutInfo(uint256 _payoutId) external view returns (Structs.Payout memory);
    function getPayoutIdsForAsset(address _assetAddress) external view returns (uint256[] memory);
    function getPayoutsForAsset(address _assetAddress) external view returns (Structs.Payout[] memory);
    function getPayoutIdsForOwner(address _ownerAddress) external view returns (uint256[] memory);
    function getPayoutsForOwner(address _ownerAddress) external view returns (Structs.Payout[] memory);
    function getAmountOfClaimedFunds(uint256 _payoutId, address _wallet) external view returns (uint256);
}
