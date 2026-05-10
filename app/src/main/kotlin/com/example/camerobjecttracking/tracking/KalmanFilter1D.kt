package com.example.camerobjecttracking.tracking

import kotlin.math.*

/**
 * Filtre de Kalman 1D pour estimer position et vélocité à partir de mesures bruitées.
 * Modèle : vitesse constante (constant velocity model)
 *
 * State vector: x = [position, velocity]^T
 * Measurement: z = position
 *
 * Equations :
 * - Predict: x_{k|k-1} = F * x_{k-1|k-1}
 * - Correct: x_{k|k} = x_{k|k-1} + K * (z - H * x_{k|k-1})
 *
 * @param processNoise variance du bruit de processus (q) — plus élevé = filtre plus réactif
 * @param measurementNoise variance du bruit de mesure (r) — plus élevé = moins de confiance aux mesures
 */
class KalmanFilter1D(
    initialPosition: Float = 0f,
    initialVelocity: Float = 0f,
    private val processNoise: Float = 0.05f,
    private val measurementNoise: Float = 0.1f
) {
    // State: [position, velocity]
    private var state = FloatArray(2) { 0f }.also {
        it[0] = initialPosition
        it[1] = initialVelocity
    }

    // Covariance error matrix (2x2)
    private var covariance = Array(2) { FloatArray(2) }.also {
        it[0][0] = 1f   // variance position
        it[0][1] = 0f   // covariance pos-vel
        it[1][0] = 0f   // covariance vel-pos
        it[1][1] = 1f   // variance velocity
    }

    /**
     * Prédit l'état suivant en supposant un pas de temps dt.
     * @param dt Delta temps en secondes (ou same unité que velocity)
     */
    fun predict(dt: Float) {
        // State transition matrix F
        // [1  dt]
        // [0  1]
        val F = arrayOf(
            floatArrayOf(1f, dt),
            floatArrayOf(0f, 1f)
        )

        // x = F * x
        val newState = FloatArray(2).also {
            it[0] = F[0][0] * state[0] + F[0][1] * state[1]
            it[1] = F[1][0] * state[0] + F[1][1] * state[1]
        }
        state = newState

        // P = F * P * F^T + Q
        // Q (process noise covariance) approximation:
        val Q = arrayOf(
            floatArrayOf(dt * dt * dt * dt / 4f, dt * dt * dt / 2f),
            floatArrayOf(dt * dt * dt / 2f, dt * dt)
        ).map { row -> row.map { it * processNoise }.toFloatArray() }.toTypedArray()

        val Ft = transpose(F)
        val FP = multiply(F, covariance)
        val FPFt = multiply(FP, Ft)

        covariance = Array(2) { i ->
            FloatArray(2) { j ->
                FPFt[i][j] + Q[i][j]
            }
        }
    }

    /**
     * Corrige l'état avec une mesure de position.
     * @param measurement Mesure de position (normalisée ou réelle)
     */
    fun correct(measurement: Float) {
        // Measurement matrix H = [1, 0]
        val H = floatArrayOf(1f, 0f)

        // Innovation: y = z - H*x
        val predictedMeasurement = H[0] * state[0] + H[1] * state[1]
        val innovation = measurement - predictedMeasurement

        // Innovation covariance: S = H * P * H^T + R
        val HP = multiplyRowVector(H, covariance) // 1x2
        val HPHt = dotProduct(HP, H) // scalaire
        val S = HPHt + measurementNoise

        // Kalman gain: K = P * H^T * S^{-1}
        // P * H^T = [P00, P10]^T * H0 = [P00*H0, P10*H0] (2x1)
        val PHt = floatArrayOf(covariance[0][0] * H[0], covariance[1][0] * H[0])
        val K = FloatArray(2) { PHt[it] / S }

        // x = x + K * innovation
        state[0] += K[0] * innovation
        state[1] += K[1] * innovation

        // P = (I - K*H) * P
        val KH = FloatArray(2) { i -> K[i] * H[i] }
        val IminusKH = Array(2) { i ->
            FloatArray(2) { j ->
                if (i == j) 1f - KH[i] else -KH[i]
            }
        }
        covariance = multiply(IminusKH, covariance)
    }

    /** Retourne l'état courant (position, velocity). */
    fun getState(): Pair<Float, Float> = state[0] to state[1]

    /** Position estimée. */
    val position: Float get() = state[0]

    /** Vélocité estimée. */
    val velocity: Float get() = state[1]

    fun reset(initialPosition: Float = 0f, initialVelocity: Float = 0f) {
        state[0] = initialPosition
        state[1] = initialVelocity
        covariance = Array(2) { FloatArray(2) }.also {
            it[0][0] = 1f; it[0][1] = 0f; it[1][0] = 0f; it[1][1] = 1f
        }
    }

    // Utilitaires algèbre linéaire simples
    private fun transpose(m: Array<FloatArray>): Array<FloatArray> = Array(2) { j ->
        FloatArray(2) { i -> m[i][j] }
    }

    private fun multiply(a: Array<FloatArray>, b: Array<FloatArray>): Array<FloatArray> {
        val result = Array(2) { FloatArray(2) }
        for (i in 0..1) {
            for (j in 0..1) {
                var sum = 0f
                for (k in 0..1) sum += a[i][k] * b[k][j]
                result[i][j] = sum
            }
        }
        return result
    }

    private fun multiplyRowVector(v: FloatArray, m: Array<FloatArray>): FloatArray {
        // v (1x2) * m (2x2) -> 1x2
        return FloatArray(2) { j ->
            v[0] * m[0][j] + v[1] * m[1][j]
        }
    }

    private fun dotProduct(v: FloatArray, w: FloatArray): Float {
        return v[0] * w[0] + v[1] * w[1]
    }
}
