/*
 * Copyright 2023 RethinkDNS and its authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.celzero.bravedns.ui

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.celzero.bravedns.databinding.DialogWgAddPeerBinding
import com.celzero.bravedns.service.WireguardManager
import com.celzero.bravedns.util.LoggerConstants
import com.celzero.bravedns.wireguard.Peer
import com.celzero.bravedns.wireguard.util.ErrorMessages

class WgAddPeerDialog(
    activity: Activity,
    themeID: Int,
    private var configId: Int,
    private val wgPeer: Peer?
) : Dialog(activity, themeID) {

    private lateinit var b: DialogWgAddPeerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        b = DialogWgAddPeerBinding.inflate(layoutInflater)
        setContentView(b.root)
        initView()
        setupClickListener()
    }

    private fun initView() {
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        if (wgPeer != null) {
            b.peerPublicKey.setText(wgPeer.getPublicKey().base64())
            if (wgPeer.getPreSharedKey().isPresent) {
                b.peerPresharedKey.setText(wgPeer.getPreSharedKey().get().toString())
            }
            b.peerAllowedIps.setText(wgPeer.getAllowedIps().joinToString { it.toString() })
            if (wgPeer.getEndpoint().isPresent) {
                b.peerEndpoint.setText(wgPeer.getEndpoint().get().toString())
            }
            if (wgPeer.persistentKeepalive.isPresent) {
                b.peerPersistentKeepAlive.setText(wgPeer.persistentKeepalive.get().toString())
            } else {
                // no-op
            }
        }
    }

    private fun setupClickListener() {
        b.customDialogDismissButton.setOnClickListener { this.dismiss() }

        b.customDialogOkButton.setOnClickListener {
            val peerPublicKey = b.peerPublicKey.text.toString()
            val presharedKey = b.peerPresharedKey.text.toString().ifEmpty { peerPublicKey }
            val peerEndpoint = b.peerEndpoint.text.toString()
            val peerPersistentKeepAlive =
                b.peerPersistentKeepAlive.text.toString().ifEmpty { "100" }
            val allowedIps = b.peerAllowedIps.text.toString()

            try {
                val wgPeer =
                    Peer.Builder()
                        .parseEndpoint(peerEndpoint)
                        .parseAllowedIPs(allowedIps)
                        .parsePersistentKeepalive(peerPersistentKeepAlive)
                        .parsePublicKey(peerPublicKey)
                        .parsePreSharedKey(presharedKey)
                        .build()
                WireguardManager.addPeer(configId, wgPeer)
                this.dismiss()
            } catch (e: Throwable) {
                Log.e(LoggerConstants.LOG_TAG_PROXY, "Error while adding peer", e)
                Toast.makeText(context, ErrorMessages[context, e], Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }
    }
}
