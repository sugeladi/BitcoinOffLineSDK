package dto.impl;

import dto.IAddress;
import entity.P2SHMultiSigAccount;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptPattern;
import sdk.BitcoinOffLineSDK;
import utils.Converter;

import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

/**
 * 比特币地址相关(集成bip32,bip39)
 */
public class AddressImpl implements IAddress {

    /**
     * 通过种子和路径获取ECKey
     * @param seed 种子
     * @param accountIndex 账户索引
     * @param addressIndex 地址索引
     * @return ECKey
     */
    public ECKey getECKey(byte[] seed, int accountIndex, int addressIndex) {
        DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
        DeterministicHierarchy deterministicHierarchy = new DeterministicHierarchy(masterPrivateKey);
        List<ChildNumber> parentPath = new ArrayList<>();
        parentPath.add(new ChildNumber(accountIndex));
        DeterministicKey deterministicKey = deterministicHierarchy.deriveChild(parentPath, false, true, new ChildNumber(addressIndex));
        return ECKey.fromPrivate(deterministicKey.getPrivKey());
    }

    /**
     * 通过ECKey获取地址
     * @param ecKey ECKey
     * @return 地址
     */
    @Override
    public String getLegacyAddress(ECKey ecKey) {
        LegacyAddress legacyAddress=LegacyAddress.fromKey(BitcoinOffLineSDK.CONFIG.getNetworkParameters(),ecKey);
        return legacyAddress.toBase58();
    }

    /**
     * 通过ECKey获取钱包可导入格式的私钥
     * @param ecKey ECKey
     * @return 钱包可导入格式的私钥
     */
    @Override
    public String getWIF(ECKey ecKey) {
        return ecKey.getPrivateKeyAsWiF(BitcoinOffLineSDK.CONFIG.getNetworkParameters());
    }

    /**
     * 通过ECKey获取公钥的十六进制格式文本
     * @param ecKey ECKey
     * @return 公钥的十六进制格式文本
     */
    @Override
    public String getPublicKeyAsHex(ECKey ecKey) {
        return ecKey.getPublicKeyAsHex();
    }

    /**
     * 通过ECKey获取私钥的十六进制格式文本
     * @param ecKey ECKey
     * @return 私钥的十六进制格式文本
     */
    @Override
    public String getPrivateKeyAsHex(ECKey ecKey) {
        return ecKey.getPrivateKeyAsHex();
    }

    /**
     * 通过ECKey获取私钥的字节数组
     * @param ecKey ECKey
     * @return 私钥的字节数组
     */
    @Override
    public byte[] getPrivateKeyBytes(ECKey ecKey) {
        return ecKey.getPrivKeyBytes();
    }

    /**
     * 通过十六进制公钥文本构建ECKey，通常用于验签
     * @param publicKeyHex 十六进制公钥文本
     * @return ECKey
     */
    @Override
    public ECKey publicKeyToECKey(String publicKeyHex) {
        return ECKey.fromPublicOnly(Converter.hexToByte(publicKeyHex));
    }

    /**
     * 通过钱包可导入格式私钥构建ECKey，通常用于导入账户
     * @param WIF 钱包可导入格式私钥
     * @return ECKey
     */
    @Override
    public ECKey WIFToECKey(String WIF) {
        return DumpedPrivateKey.fromBase58(BitcoinOffLineSDK.CONFIG.getNetworkParameters(), WIF).getKey();
    }

    /**
     * 生成多签地址
     * @param threshold 最低签名数量
     * @param keys 公钥构建的ECKey列表
     * @return P2SHMultiSigAccount对象
     */
    @Override
    public P2SHMultiSigAccount generateMultiSigAddress(int threshold, List<ECKey> keys) {
        //创建多签赎回脚本
        Script redeemScript = ScriptBuilder.createRedeemScript(threshold, keys);
        //为给定的赎回脚本创建scriptPubKey
        Script script = ScriptBuilder.createP2SHOutputScript(redeemScript);
        //返回一个地址，该地址表示从给定的scriptPubKey中提取的脚本HASH
        byte[] scriptHash=ScriptPattern.extractHashFromP2SH(script);
        LegacyAddress multiSigAddress=LegacyAddress.fromScriptHash(BitcoinOffLineSDK.CONFIG.getNetworkParameters(),scriptHash);
//        Address multiSigAddress = Address.fromP2SHScript(BitcoinOffLineSDK.CONFIG.getNetworkParameters(), script);
        return new P2SHMultiSigAccount(redeemScript, multiSigAddress);
    }

    /**
     * 消息签名
     * @param WIF 钱包可导入模式的私钥
     * @param message 待签名消息
     * @return 签名后的base64文本
     */
    @Override
    public String signMessage(String WIF, String message) {
        ECKey ecKey=WIFToECKey(WIF);
        return ecKey.signMessage(message);
    }

    /**
     * 消息验签
     * @param publicKeyHex 十六进制公钥
     * @param message 消息
     * @param signatureBase64 签名base64格式文本
     * @return 结果
     */
    @Override
    public boolean verifyMessage(String publicKeyHex, String message, String signatureBase64) {
        ECKey ecKey = BitcoinOffLineSDK.ADDRESS.publicKeyToECKey(publicKeyHex);
        try {
            ecKey.verifyMessage(message,signatureBase64);
        } catch (SignatureException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
