package com.example.data.evidence

import com.example.data.local.EvidenceRecordEntity
import com.example.data.local.SpectraDao
import java.util.UUID

class EvidenceRepository(private val dao: SpectraDao) {
    suspend fun append(draft: EvidenceDraft): EvidenceRecordEntity {
        require(draft.classification != EvidenceClassification.AI_INTERPRETATION)
        val previous = dao.getLatestEvidence()
        val now = System.currentTimeMillis()
        val hash = EvidenceHasher.sha256(EvidenceHasher.canonical(
            draft.sourceType, draft.classification, draft.title, draft.payload,
            draft.locationLabel, now, previous?.sha256
        ))
        val record = EvidenceRecordEntity(
            evidenceId = UUID.randomUUID().toString(), sourceType = draft.sourceType,
            classification = draft.classification.name, title = draft.title, payload = draft.payload,
            locationLabel = draft.locationLabel, createdAt = now, previousHash = previous?.sha256,
            sha256 = hash, supportingEvidenceIds = ""
        )
        dao.insertEvidence(record)
        return record
    }

    suspend fun appendDerived(classification: EvidenceClassification, title: String, payload: String,
                               supportingEvidenceIds: List<String>, locationLabel: String? = null): EvidenceRecordEntity {
        require(classification != EvidenceClassification.MEASURED)
        require(supportingEvidenceIds.isNotEmpty())
        val existing = dao.getEvidenceByIds(supportingEvidenceIds)
        require(existing.map { it.evidenceId }.toSet() == supportingEvidenceIds.toSet())
        val previous = dao.getLatestEvidence()
        val now = System.currentTimeMillis()
        val hash = EvidenceHasher.sha256(EvidenceHasher.canonical(
            "DERIVED", classification, title, payload, locationLabel, now, previous?.sha256
        ))
        val record = EvidenceRecordEntity(
            evidenceId = UUID.randomUUID().toString(), sourceType = "DERIVED",
            classification = classification.name, title = title, payload = payload,
            locationLabel = locationLabel, createdAt = now, previousHash = previous?.sha256,
            sha256 = hash, supportingEvidenceIds = supportingEvidenceIds.joinToString(",")
        )
        dao.insertEvidence(record)
        return record
    }
}
