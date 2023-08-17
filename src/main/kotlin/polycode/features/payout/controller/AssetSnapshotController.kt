package polycode.features.payout.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import polycode.config.binding.annotation.ApiKeyBinding
import polycode.config.interceptors.annotation.ApiReadLimitedMapping
import polycode.config.interceptors.annotation.ApiWriteLimitedMapping
import polycode.config.interceptors.annotation.IdType
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.result.Project
import polycode.features.payout.model.params.CreateAssetSnapshotParams
import polycode.features.payout.model.request.CreateAssetSnapshotRequest
import polycode.features.payout.model.response.AssetSnapshotResponse
import polycode.features.payout.model.response.AssetSnapshotsResponse
import polycode.features.payout.model.response.CreateAssetSnapshotResponse
import polycode.features.payout.service.AssetSnapshotQueueService
import polycode.features.payout.util.AssetSnapshotStatus
import polycode.generated.jooq.id.AssetSnapshotId
import polycode.generated.jooq.id.ProjectId
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.WalletAddress
import javax.validation.Valid

@Validated
@RestController
class AssetSnapshotController(private val snapshotQueueService: AssetSnapshotQueueService) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/asset-snapshots")
    fun createAssetSnapshot(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAssetSnapshotRequest
    ): ResponseEntity<CreateAssetSnapshotResponse> {
        val assetSnapshotId = snapshotQueueService.submitAssetSnapshot(
            CreateAssetSnapshotParams(
                name = requestBody.name,
                chainId = project.chainId,
                projectId = project.id,
                assetContractAddress = ContractAddress(requestBody.assetAddress),
                payoutBlock = BlockNumber(requestBody.payoutBlockNumber),
                ignoredHolderAddresses = requestBody.ignoredHolderAddresses.mapTo(HashSet()) { WalletAddress(it) }
            )
        )

        return ResponseEntity.ok(CreateAssetSnapshotResponse(assetSnapshotId))
    }

    @ApiReadLimitedMapping(IdType.ASSET_SNAPSHOT_ID, "/v1/asset-snapshots/{id}")
    fun getAssetSnapshotById(
        @PathVariable id: AssetSnapshotId
    ): ResponseEntity<AssetSnapshotResponse> {
        return snapshotQueueService.getAssetSnapshotById(id)
            ?.let { ResponseEntity.ok(it.toAssetSnapshotResponse()) }
            ?: throw ResourceNotFoundException("Asset snapshot not found")
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/asset-snapshots/by-project/{projectId}")
    fun getAssetSnapshots(
        @PathVariable("projectId") projectId: ProjectId,
        @RequestParam(required = false) status: List<AssetSnapshotStatus>?
    ): ResponseEntity<AssetSnapshotsResponse> {
        val assetSnapshots = snapshotQueueService.getAllAssetSnapshotsByProjectIdAndStatuses(
            projectId = projectId,
            statuses = status.orEmpty().toSet()
        )

        return ResponseEntity.ok(AssetSnapshotsResponse(assetSnapshots.map { it.toAssetSnapshotResponse() }))
    }
}
