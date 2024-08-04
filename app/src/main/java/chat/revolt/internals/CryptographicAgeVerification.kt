package chat.revolt.internals

import java.security.MessageDigest

class CryptographicAgeVerification {
    fun getProofHash(
        seed: String,
        userAge: Int,
        minAllowedAge: Int
    ): Pair<ByteArray, ByteArray> {
        var proof = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())

        for (i in 1 until userAge - minAllowedAge) {
            proof = MessageDigest.getInstance("SHA-256").digest(proof)
        }

        var ageHash =
            MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())

        for (i in 1 until userAge - 1) {
            ageHash = MessageDigest.getInstance("SHA-256").digest(ageHash)
        }

        return ageHash to proof
    }

    fun proveAge(
        minAllowedAge: Int,
        proofHash: ByteArray,
        ageHash: ByteArray,
    ): Boolean {
        var verifyHash =
            MessageDigest.getInstance("SHA-256").digest(proofHash.toString().toByteArray())

        for (i in 1 until minAllowedAge - 1) {
            verifyHash = MessageDigest.getInstance("SHA-256").digest(verifyHash)
        }

        return verifyHash.contentEquals(ageHash)
    }
}