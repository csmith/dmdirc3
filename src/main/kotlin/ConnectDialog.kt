package com.dmdirc

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ButtonBar
import org.kodein.di.generic.instance
import tornadofx.*

class ServerlistDialog : Fragment() {
    private val model = ConnectionDetailsModel()
    override val root = form {
        borderpane {
            minWidth = 600.toDouble()
            minHeight = 400.toDouble()
            left = listview(model.servers) {
                bindSelected(model)
                if (model.servers.isNotEmpty()) {
                    selectionModel.select(model.servers[0])
                }
                cellFormat {
                    text = if (it.hostname.isEmpty()) {
                        "[Empty]"
                    } else {
                        it.hostname
                    }
                }
            }
            bottom = buttonbar {
                button("Add", ButtonBar.ButtonData.LEFT) {
                    action {
                        model.servers.add(ConnectionDetails("", "", 6667, tls = false, autoconnect = false))
                    }
                }
                button("Delete", ButtonBar.ButtonData.LEFT) {
                    action {
                        model.servers.remove(model.item)
                    }
                }
                button("Save", ButtonBar.ButtonData.OK_DONE) {
                    enableWhen(model.dirty)
                    action {
                        model.commit()
                        close()
                    }
                }
                button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                    action {
                        model.rollback()
                        close()
                    }
                }
            }
            center = ServerInfoPane(model).root
        }
    }
}

class ConnectionDetailsModel : ItemViewModel<ConnectionDetails>() {
    private val config1 by kodein.instance<ClientConfig>()

    val servers = config1[ClientSpec.servers].toMutableList().observable()
    val hostname = bind(ConnectionDetails::hostname)
    val password = bind(ConnectionDetails::password)
    val port = bind(ConnectionDetails::port)
    val tls = bind(ConnectionDetails::tls)
    val autoconnect = bind(ConnectionDetails::autoconnect)

    override fun onCommit() {
        config1[ClientSpec.servers] = servers
        config1.save()
    }
}

class ConnectDialog : Fragment() {
    private val controller: MainController by inject()
    private val config1 by kodein.instance<ClientConfig>()

    private val servers = config1[ClientSpec.servers].toMutableList().observable()
    private val selected = SimpleObjectProperty<ConnectionDetails>()

    override val root = form {
        combobox(selected, servers) {
            bindSelected(selected)
            cellFormat {
                text = "${it.hostname}:${it.port}"
            }
            if (servers.isNotEmpty()) {
                selectionModel.select(servers[0])
            }
        }
        buttonbar {
            button("Connect", ButtonBar.ButtonData.OK_DONE) {
                action {
                    controller.connect(selected.value)
                    close()
                }
            }
            button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                action {
                    close()
                }
            }
        }
    }
}

class ServerInfoPane(private val model: ConnectionDetailsModel) : Fragment() {
    override val root = form {
        fieldset {
            field("Server Name") {
                textfield(model.hostname) {
                    enableWhen(!model.empty)
                    required()
                }
            }
            field("Port") {
                spinner(editable = true, property = model.port, min = 1, max = 65535) {
                    enableWhen(!model.empty)
                }
            }
            field("Password") {
                textfield(model.password) {
                    enableWhen(!model.empty)
                }
            }
            field("TLS") {
                checkbox(property = model.tls) {
                    enableWhen(!model.empty)
                }
            }
            field("Auto Connect") {
                checkbox(property = model.autoconnect) {
                    enableWhen(!model.empty)
                }
            }
        }
    }
}