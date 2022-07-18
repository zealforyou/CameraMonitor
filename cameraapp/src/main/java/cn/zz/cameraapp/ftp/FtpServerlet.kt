package cn.zz.cameraapp.ftp

import androidx.annotation.Keep
import cn.zz.cameraapp.usb.UsbStorage
import me.jahnen.libaums.core.fs.UsbFile
import java.io.File

import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.listener.ListenerFactory

import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.filesystem.tfp.UsbFileSystemFactory
import org.apache.ftpserver.ftplet.*
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor

import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.apache.ftpserver.usermanager.impl.BaseUser

/**
 *
 * Created by ZhangZhuo on 2022/7/12.
 */
@Keep
class FtpServerlet(
    private val sharedDirectory: String,
    private val ftpPort: Int = 2121,
    private val log: ((tag: String, content: String?) -> Unit)? = null)
    : DefaultFtplet()
{
    private val TAG = "FtpServerlet"
    private var mFtpServer: FtpServer? = null
    private val mUser = "root"
    private val mPassword = "root"

    fun startFtp() {
        if (null != mFtpServer && !mFtpServer!!.isStopped) {
            return
        }
        val file = File(sharedDirectory)
        if (!file.exists()) {
            file.mkdirs()
        }

        val serverFactory = kotlin.runCatching {
            FtpServerFactory().apply {
                if (UsbStorage.rootFile != null) {
                    fileSystem = UsbFileSystemFactory()
                }
            }
        }.onFailure {
            it.printStackTrace()
        }.getOrNull() ?: return
        val listenerFactory = kotlin.runCatching {
            ListenerFactory()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull() ?: return

        // 设定端末番号
        listenerFactory.port = ftpPort

        // 通过PropertiesUserManagerFactory创建UserManager然后向配置文件添加用户
        val userManagerFactory = PropertiesUserManagerFactory()
        userManagerFactory.passwordEncryptor = SaltedPasswordEncryptor()

        val userManager: UserManager = userManagerFactory.createUserManager()
        val auths = mutableListOf<Authority>()
        val auth: Authority = WritePermission()
        auths.add(auth)
        //添加用户
        val user = BaseUser()
        user.name = mUser
        user.password = mPassword
        user.homeDirectory = sharedDirectory
        user.usbFileHome = UsbStorage.rootFile
        user.authorities = auths
        userManager.save(user)

        // 设定Ftplet
        val ftpletMap = mutableMapOf<String, Ftplet>()
        ftpletMap["Ftplet"] = this
        serverFactory.userManager = userManager
        serverFactory.addListener("default", listenerFactory.createListener())
//        serverFactory.ftplets = ftpletMap
        mFtpServer = serverFactory.createServer()
        kotlin.runCatching {
            mFtpServer?.start()
        }.onFailure {
            it.printStackTrace()
        }

        log?.invoke(TAG, "start ftp server ， sharedDirectory = $sharedDirectory, usbFileHome=${UsbStorage.rootFile?.absolutePath}")
    }

    fun stopFtp() {
        // FtpServer不存在和FtpServer正在运行中
        if (null != mFtpServer && !mFtpServer!!.isStopped) {
            mFtpServer?.stop()
            log?.invoke(TAG, "stop ftp server")
        }
    }

    override fun onAppendStart(session: FtpSession?, request: FtpRequest?): FtpletResult {
        log?.invoke(
            TAG,
            "onAppendStart  argument = ${request?.argument} , requestLine = ${request?.requestLine} , command = ${request?.command}"
        )
        return super.onAppendStart(session, request)
    }

    override fun onAppendEnd(session: FtpSession?, request: FtpRequest?): FtpletResult {
        log?.invoke(
            TAG,
            "onAppendEnd  argument = ${request?.argument} , requestLine = ${request?.requestLine} , command = ${request?.command}"
        )
        return super.onAppendEnd(session, request)
    }

    override fun onLogin(session: FtpSession?, request: FtpRequest?): FtpletResult {
        log?.invoke(
            TAG,
            "onLogin  argument = ${request?.argument} , requestLine = ${request?.requestLine} , command = ${request?.command}"
        )
        return super.onLogin(session, request)
    }

    override fun onConnect(session: FtpSession?): FtpletResult {
        log?.invoke(TAG, "onConnect ")
        return super.onConnect(session)
    }

    override fun onDisconnect(session: FtpSession?): FtpletResult {
        log?.invoke(TAG, "onDisconnect ")
        return super.onDisconnect(session)
    }

    override fun onUploadStart(session: FtpSession?, request: FtpRequest?): FtpletResult {
        log?.invoke(
            TAG,
            "onUploadStart  argument = ${request?.argument} , requestLine = ${request?.requestLine} , command = ${request?.command}"
        )
        return super.onUploadStart(session, request)
    }

    override fun onUploadEnd(session: FtpSession?, request: FtpRequest?): FtpletResult {
        log?.invoke(
            TAG,
            "onUploadEnd  argument = ${request?.argument} , requestLine = ${request?.requestLine} , command = ${request?.command}"
        )
        return super.onUploadEnd(session, request)
    }

    override fun onDownloadStart(session: FtpSession?, request: FtpRequest?): FtpletResult {
        log?.invoke(
            TAG,
            "onDownloadStart  argument = ${request?.argument} , requestLine = ${request?.requestLine} , command = ${request?.command}"
        )
        return super.onDownloadStart(session, request)
    }

    override fun onDownloadEnd(session: FtpSession?, request: FtpRequest?): FtpletResult {
        log?.invoke(
            TAG,
            "onDownloadEnd  argument = ${request?.argument} , requestLine = ${request?.requestLine} , command = ${request?.command}"
        )
        return super.onDownloadEnd(session, request)
    }
}
