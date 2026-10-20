package io.github.damienllado.snapkeymapper;

import android.content.Context;
import android.os.Build;
import android.sun.security.x509.AlgorithmId;
import android.sun.security.x509.CertificateAlgorithmId;
import android.sun.security.x509.CertificateExtensions;
import android.sun.security.x509.CertificateIssuerName;
import android.sun.security.x509.CertificateSerialNumber;
import android.sun.security.x509.CertificateSubjectName;
import android.sun.security.x509.CertificateValidity;
import android.sun.security.x509.CertificateVersion;
import android.sun.security.x509.CertificateX509Key;
import android.sun.security.x509.KeyIdentifier;
import android.sun.security.x509.PrivateKeyUsageExtension;
import android.sun.security.x509.SubjectKeyIdentifierExtension;
import android.sun.security.x509.X500Name;
import android.sun.security.x509.X509CertImpl;
import android.sun.security.x509.X509CertInfo;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;

/** ADB identity of the app (a key pair made on the phone, kept only until the setup ends). */
final class Adb extends AbsAdbConnectionManager {
    private final PrivateKey key;
    private final Certificate cert;

    Adb(Context c) throws Exception {
        setApi(Build.VERSION.SDK_INT);
        File k = new File(c.getFilesDir(), "adbkey.pk8"), crt = new File(c.getFilesDir(), "adbkey.crt");
        if (!k.exists() || !crt.exists()) {
            KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
            String algo = "SHA512withRSA";
            Date from = new Date(), to = new Date(from.getTime() + 86400000L * 365);
            X500Name name = new X500Name("CN=SnapKey Mapper");
            CertificateExtensions ext = new CertificateExtensions();
            ext.set("SubjectKeyIdentifier", new SubjectKeyIdentifierExtension(new KeyIdentifier(pair.getPublic()).getIdentifier()));
            ext.set("PrivateKeyUsage", new PrivateKeyUsageExtension(from, to));
            X509CertInfo info = new X509CertInfo();
            info.set("version", new CertificateVersion(2));
            info.set("serialNumber", new CertificateSerialNumber(new SecureRandom().nextInt() & Integer.MAX_VALUE));
            info.set("algorithmID", new CertificateAlgorithmId(AlgorithmId.get(algo)));
            info.set("subject", new CertificateSubjectName(name));
            info.set("issuer", new CertificateIssuerName(name));
            info.set("key", new CertificateX509Key(pair.getPublic()));
            info.set("validity", new CertificateValidity(from, to));
            info.set("extensions", ext);
            X509CertImpl made = new X509CertImpl(info);
            made.sign(pair.getPrivate(), algo);
            Files.write(k.toPath(), pair.getPrivate().getEncoded());
            Files.write(crt.toPath(), made.getEncoded());
        }
        key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(k.toPath())));
        try (FileInputStream in = new FileInputStream(crt)) {
            cert = CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    /** Forgets the identity: the phone keeps a stale entry in its paired devices that nobody can use any more. */
    static void forget(Context c) {
        new File(c.getFilesDir(), "adbkey.pk8").delete();
        new File(c.getFilesDir(), "adbkey.crt").delete();
    }

    @Override protected PrivateKey getPrivateKey() { return key; }
    @Override protected Certificate getCertificate() { return cert; }
    @Override protected String getDeviceName() { return "SnapKey Mapper"; }
}
