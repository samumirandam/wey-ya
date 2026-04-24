package com.weyya.app.data.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SimInfo(
    val slotIndex: Int,
    val carrierName: String,
)

@Singleton
class SimResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val telephonyManager: TelephonyManager? =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val subscriptionManager: SubscriptionManager? =
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

    private val telecomManager: TelecomManager? =
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    fun hasPhonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Returns true if the device reports more than one active SIM modem.
     * Does NOT require READ_PHONE_STATE: uses activeModemCount / phoneCount which are safe.
     */
    fun hasDualSim(): Boolean {
        val tm = telephonyManager ?: return false
        val count = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tm.activeModemCount
        } else {
            @Suppress("DEPRECATION")
            tm.phoneCount
        }
        return count > 1
    }

    /**
     * Returns active SIMs ordered by slot. Requires READ_PHONE_STATE; returns empty
     * if the permission is not granted, on mono-SIM devices, or if lookup fails.
     */
    @SuppressLint("MissingPermission")
    fun getActiveSims(): List<SimInfo> {
        if (!hasPhonePermission()) return emptyList()
        val sm = subscriptionManager ?: return emptyList()
        val list = try {
            sm.activeSubscriptionInfoList ?: emptyList()
        } catch (_: SecurityException) {
            return emptyList()
        }
        return list
            .map {
                SimInfo(
                    slotIndex = it.simSlotIndex,
                    carrierName = it.carrierName?.toString().orEmpty(),
                )
            }
            .sortedBy { it.slotIndex }
    }

    /**
     * Maps a Call.Details to the SIM slot that received the call (0 or 1).
     * Returns null on any failure so the caller falls back to "applies to all SIMs".
     *
     * Tries, in order:
     *  1. API 30+ direct lookup via TelephonyManager.getSubscriptionId(handle), then matches
     *     the subscription id against SubscriptionManager.activeSubscriptionInfoList.
     *  2. Heuristic match by handle.id — on most OEMs this is the iccId or the subscription
     *     id as a string.
     *  3. Single-SIM shortcut — if only one subscription is active, assume it owns the call.
     *  4. Last-ditch match by PhoneAccount.address (the SIM's phone number, when exposed).
     */
    @SuppressLint("MissingPermission")
    fun resolveSlotFromCallDetails(callDetails: Call.Details): Int? {
        val handle = callDetails.accountHandle ?: return null
        if (!hasPhonePermission()) return null

        val sm = subscriptionManager ?: return null
        val subs = try {
            sm.activeSubscriptionInfoList ?: return null
        } catch (_: SecurityException) {
            return null
        }

        // API 30+: direct subscriptionId lookup from the PhoneAccountHandle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val subId = telephonyManager?.getSubscriptionId(handle)
                if (subId != null && subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    val match = subs.firstOrNull { it.subscriptionId == subId }
                    if (match != null) return match.simSlotIndex
                }
            } catch (_: SecurityException) {
                // Fall through to id/number heuristics
            } catch (_: IllegalStateException) {
                // Fall through
            }
        }

        // Heuristic fallback for pre-R or when the direct lookup fails:
        // PhoneAccount.id is usually iccId or the subscription id as string.
        val handleId = handle.id
        val bySub = subs.firstOrNull { info ->
            info.iccId == handleId || info.subscriptionId.toString() == handleId
        }
        if (bySub != null) return bySub.simSlotIndex

        // If only one active SIM, assume it owns the call.
        if (subs.size == 1) return subs[0].simSlotIndex

        // Last resort: match by PhoneAccount address (the SIM's phone number, when exposed).
        val phoneAccount = try {
            telecomManager?.getPhoneAccount(handle)
        } catch (_: SecurityException) {
            null
        }
        val accountAddress = phoneAccount?.address?.schemeSpecificPart
        if (!accountAddress.isNullOrBlank()) {
            val byNumber = subs.firstOrNull { it.number == accountAddress }
            if (byNumber != null) return byNumber.simSlotIndex
        }

        return null
    }
}
