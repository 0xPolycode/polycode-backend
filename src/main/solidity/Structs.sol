// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./IERC20.sol";

interface Structs {
    struct Payout {
        uint256 payoutId; // ID of this payout
        address payoutOwner; // address which created this payout
        string payoutInfo; // payout info (or IPFS hash for info)
        bool isCanceled; // determines if this payout is canceled

        IERC20 asset; // asset for which payout is being made
        uint256 totalAssetAmount; // sum of all asset holdings in the snapshot, minus ignored asset address holdings
        address[] ignoredHolderAddresses; // addresses which aren't included in the payout

        bytes32 assetSnapshotMerkleRoot; // Merkle root hash of asset holdings in the snapshot, without ignored addresses
        uint256 assetSnapshotMerkleDepth; // depth of snapshot Merkle tree
        uint256 assetSnapshotBlockNumber; // snapshot block number
        string assetSnapshotMerkleIpfsHash; // IPFS hash of stored asset snapshot Merkle tree

        IERC20 rewardAsset; // asset issued as payout reward
        uint256 totalRewardAmount; // total amount of reward asset in this payout
        uint256 remainingRewardAmount; // remaining reward asset amount in this payout
    }
}
