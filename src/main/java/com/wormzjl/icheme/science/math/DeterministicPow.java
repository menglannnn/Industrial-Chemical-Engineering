package com.wormzjl.icheme.science.math;

/**
 * {@code x^y} for {@link DeterministicMath#pow}: a table-driven kernel in plain IEEE double arithmetic, faster than
 * fdlibm's {@code e_pow.c} (batch 2026-09-27-jdk-determinism, candidate C of POW_PERFORMANCE_REVIEW.md).
 *
 * <p>Determinism: the fast path uses only {@code + - * /}, comparisons, {@code Math.abs}, {@code Math.rint}, raw bit
 * conversions and lookups in the constant tables below, each exactly specified by the JLS (strict IEEE 754 since
 * Java 17), and no fused multiply-add; so every conforming JVM, operating system and CPU returns the same bits.
 * Accuracy: within 1 ulp of {@code StrictMath.pow} and about 0.5 ulp of the exact value (measured on 10^7 inputs per
 * class by {@code tools/jdk-determinism/perf/src/FastDetCheck.java}); {@code DeterministicMathTest} pins bit patterns.
 * Every input outside the fast domain (x not a positive normal finite number, y zero, infinite, NaN or of magnitude
 * 2^30 or more, |y ln x| above 708) is {@link FdlibmPow}, so the special cases are {@code StrictMath.pow}'s exactly.
 *
 * <p>Method (Tang): x = 2^k m, F = 1 + j/128 nearest to m, u = (m - F)/F with its rounding residual, ln x = k ln2 +
 * ln F + ln(1+u) (degree-8 series) as a double-double; y ln x by an exact Dekker product; exp by n = rint(t 128/ln2),
 * r = t - n ln2/128, e^t = 2^(n>>7) 2^((n&127)/128) (1 + expm1 r) (degree-6 series). The tables are the correctly
 * rounded high and low parts of 2^(j/128), 1/F and ln F, printed as exact hexadecimal literals from BigDecimal series by
 * {@code tools/jdk-determinism/perf/src/PowTablesPrinter.java}; {@code DeterministicMathTest} regenerates and compares
 * them.
 */
public final class DeterministicPow {
    private DeterministicPow() {
    }

    private static final double LN2_HI = 0x1.62e42fefa3p-1;
    private static final double LN2_LO = 0x1.3de6af278ece6p-42;
    private static final double L_HI = 0x1.62e42fefp-8;
    private static final double L_LO = 0x1.473de6af278edp-41;
    private static final double INV_L = 0x1.71547652b82fep7;
    private static final double[] EXP_HI = {
        0x1.0p0, 0x1.0163da9fb3335p0, 0x1.02c9a3e778061p0, 0x1.04315e86e7f85p0,
        0x1.059b0d3158574p0, 0x1.0706b29ddf6dep0, 0x1.0874518759bc8p0, 0x1.09e3ecac6f383p0,
        0x1.0b5586cf9890fp0, 0x1.0cc922b7247f7p0, 0x1.0e3ec32d3d1a2p0, 0x1.0fb66affed31bp0,
        0x1.11301d0125b51p0, 0x1.12abdc06c31ccp0, 0x1.1429aaea92dep0, 0x1.15a98c8a58e51p0,
        0x1.172b83c7d517bp0, 0x1.18af9388c8deap0, 0x1.1a35beb6fcb75p0, 0x1.1bbe084045cd4p0,
        0x1.1d4873168b9aap0, 0x1.1ed5022fcd91dp0, 0x1.2063b88628cd6p0, 0x1.21f49917ddc96p0,
        0x1.2387a6e756238p0, 0x1.251ce4fb2a63fp0, 0x1.26b4565e27cddp0, 0x1.284dfe1f56381p0,
        0x1.29e9df51fdee1p0, 0x1.2b87fd0dad99p0, 0x1.2d285a6e4030bp0, 0x1.2ecafa93e2f56p0,
        0x1.306fe0a31b715p0, 0x1.32170fc4cd831p0, 0x1.33c08b26416ffp0, 0x1.356c55f929ff1p0,
        0x1.371a7373aa9cbp0, 0x1.38cae6d05d866p0, 0x1.3a7db34e59ff7p0, 0x1.3c32dc313a8e5p0,
        0x1.3dea64c123422p0, 0x1.3fa4504ac801cp0, 0x1.4160a21f72e2ap0, 0x1.431f5d950a897p0,
        0x1.44e086061892dp0, 0x1.46a41ed1d0057p0, 0x1.486a2b5c13cdp0, 0x1.4a32af0d7d3dep0,
        0x1.4bfdad5362a27p0, 0x1.4dcb299fddd0dp0, 0x1.4f9b2769d2ca7p0, 0x1.516daa2cf6642p0,
        0x1.5342b569d4f82p0, 0x1.551a4ca5d920fp0, 0x1.56f4736b527dap0, 0x1.58d12d497c7fdp0,
        0x1.5ab07dd485429p0, 0x1.5c9268a5946b7p0, 0x1.5e76f15ad2148p0, 0x1.605e1b976dc09p0,
        0x1.6247eb03a5585p0, 0x1.6434634ccc32p0, 0x1.6623882552225p0, 0x1.68155d44ca973p0,
        0x1.6a09e667f3bcdp0, 0x1.6c012750bdabfp0, 0x1.6dfb23c651a2fp0, 0x1.6ff7df9519484p0,
        0x1.71f75e8ec5f74p0, 0x1.73f9a48a58174p0, 0x1.75feb564267c9p0, 0x1.780694fde5d3fp0,
        0x1.7a11473eb0187p0, 0x1.7c1ed0130c132p0, 0x1.7e2f336cf4e62p0, 0x1.80427543e1a12p0,
        0x1.82589994cce13p0, 0x1.8471a4623c7adp0, 0x1.868d99b4492edp0, 0x1.88ac7d98a6699p0,
        0x1.8ace5422aa0dbp0, 0x1.8cf3216b5448cp0, 0x1.8f1ae99157736p0, 0x1.9145b0b91ffc6p0,
        0x1.93737b0cdc5e5p0, 0x1.95a44cbc8520fp0, 0x1.97d829fde4e5p0, 0x1.9a0f170ca07bap0,
        0x1.9c49182a3f09p0, 0x1.9e86319e32323p0, 0x1.a0c667b5de565p0, 0x1.a309bec4a2d33p0,
        0x1.a5503b23e255dp0, 0x1.a799e1330b358p0, 0x1.a9e6b5579fdbfp0, 0x1.ac36bbfd3f37ap0,
        0x1.ae89f995ad3adp0, 0x1.b0e07298db666p0, 0x1.b33a2b84f15fbp0, 0x1.b59728de5593ap0,
        0x1.b7f76f2fb5e47p0, 0x1.ba5b030a1064ap0, 0x1.bcc1e904bc1d2p0, 0x1.bf2c25bd71e09p0,
        0x1.c199bdd85529cp0, 0x1.c40ab5fffd07ap0, 0x1.c67f12e57d14bp0, 0x1.c8f6d9406e7b5p0,
        0x1.cb720dcef9069p0, 0x1.cdf0b555dc3fap0, 0x1.d072d4a07897cp0, 0x1.d2f87080d89f2p0,
        0x1.d5818dcfba487p0, 0x1.d80e316c98398p0, 0x1.da9e603db3285p0, 0x1.dd321f301b46p0,
        0x1.dfc97337b9b5fp0, 0x1.e264614f5a129p0, 0x1.e502ee78b3ff6p0, 0x1.e7a51fbc74c83p0,
        0x1.ea4afa2a490dap0, 0x1.ecf482d8e67f1p0, 0x1.efa1bee615a27p0, 0x1.f252b376bba97p0,
        0x1.f50765b6e454p0, 0x1.f7bfdad9cbe14p0, 0x1.fa7c1819e90d8p0, 0x1.fd3c22b8f71f1p0,
    };
    private static final double[] EXP_LO = {
        0x0.0p0, 0x1.b61299ab8cdb7p-54, -0x1.19083535b085dp-56, -0x1.0a31c1977c96ep-54,
        0x1.d73e2a475b465p-55, -0x1.c91dfe2b13c27p-55, 0x1.186be4bb284ffp-57, 0x1.1487818316136p-54,
        0x1.8a62e4adc610bp-54, 0x1.01edc16e24f71p-54, 0x1.03a1727c57b53p-59, -0x1.b9bedc44ebd7bp-57,
        -0x1.6c51039449b3ap-54, -0x1.1b514b36ca5c7p-58, -0x1.32fbf9af1369ep-54, 0x1.2406ab9eeab0ap-55,
        -0x1.19041b9d78a76p-55, -0x1.11023d1970f6cp-54, 0x1.e5b4c7b4968e4p-55, -0x1.95386352ef607p-54,
        0x1.e016e00a2643cp-54, -0x1.1df98027bb78cp-54, 0x1.dc775814a8495p-55, 0x1.2a97e9494a5eep-55,
        0x1.9b07eb6c70573p-54, 0x1.ac155bef4f4a4p-55, 0x1.2bd339940e9d9p-55, -0x1.a4c3a8c3f0d7ep-54,
        0x1.612e8afad1255p-55, -0x1.10adcd6381aa4p-59, 0x1.0024754db41d5p-54, 0x1.1ca0f45d52383p-56,
        0x1.6f46ad23182e4p-55, 0x1.a9ce78e18047cp-55, 0x1.32721843659a6p-54, -0x1.b5cee5c4e4628p-55,
        -0x1.63aeabf42eae2p-54, -0x1.e958d3c9904bdp-54, -0x1.5e436d661f5e3p-56, -0x1.efff8375d29c3p-54,
        0x1.ada0911f09ebcp-55, -0x1.7d023f956f9f3p-54, -0x1.ef3691c309278p-58, -0x1.1c7dde35f7999p-55,
        0x1.89b7a04ef80dp-59, 0x1.c944bd1648a76p-54, 0x1.3c1a3b69062fp-56, 0x1.9cb62f3d1be56p-54,
        0x1.d4397afec42e2p-56, 0x1.8ecdbbc6a7833p-54, -0x1.4b309d25957e3p-54, -0x1.f768569bd93efp-55,
        -0x1.07abe1db13cadp-55, -0x1.d689cefede59bp-55, 0x1.9bb2c011d93adp-54, 0x1.295e15b9a1de8p-55,
        0x1.6324c054647adp-54, 0x1.c4b1b816986a2p-60, 0x1.ba6f93080e65ep-54, -0x1.3e2429b56de47p-54,
        -0x1.383c17e40b497p-54, -0x1.c483c759d8933p-55, -0x1.bb60987591c34p-54, 0x1.038ae44f73e65p-57,
        -0x1.bdd3413b26456p-54, -0x1.2895667ff0b0dp-56, -0x1.bbe3a683c88abp-57, -0x1.83c0f25860ef6p-55,
        -0x1.16e4786887a99p-55, -0x1.0a8d96c65d53cp-54, -0x1.0245957316dd3p-54, 0x1.866b80a02162dp-54,
        -0x1.41577ee04992fp-55, 0x1.f124cd1164dd6p-54, 0x1.05d02ba15797ep-56, -0x1.27c86626d972bp-54,
        -0x1.d4c1dd41532d8p-54, -0x1.8d684a341cdfbp-55, -0x1.fc6f89bd4f6bap-54, 0x1.994c2f37cb53ap-54,
        0x1.6e9f156864b27p-54, -0x1.0d55e32e9e3aap-56, 0x1.5cc13a2e3976cp-55, -0x1.dd6792e582524p-54,
        -0x1.75fc781b57ebcp-57, -0x1.64b7c96a5f039p-56, -0x1.d185b7c1b85d1p-54, -0x1.173bd91cee632p-54,
        0x1.c7c46b071f2bep-56, 0x1.824ca78e64c6ep-56, -0x1.359495d1cd533p-54, 0x1.6305c7ddc36abp-54,
        -0x1.d2f6edb8d41e1p-54, 0x1.bcb7ecac563c7p-54, 0x1.0fac90ef7fd31p-54, -0x1.f9234cae76cdp-55,
        0x1.7a1cd345dcc81p-54, -0x1.bdef54c80e425p-54, -0x1.2805e3084d708p-57, -0x1.c71dfbbba6de3p-54,
        -0x1.5584f7e54ac3bp-56, -0x1.efcd30e54292ep-54, 0x1.23dd07a2d9e84p-55, -0x1.efdca3f6b9c73p-54,
        0x1.11065895048ddp-55, 0x1.b4537e083c60ap-54, 0x1.2884dff483cadp-54, 0x1.1acbc48805c44p-56,
        0x1.503cbd1e949dbp-56, -0x1.dd83b53829d72p-55, -0x1.cbc3743797a9cp-54, -0x1.d487b719d8578p-54,
        0x1.2ed02d75b3707p-55, -0x1.11ec18beddfe8p-54, 0x1.c2300696db532p-54, 0x1.2da5778f018c3p-54,
        -0x1.1a5cd4f184b5cp-54, -0x1.7b627817a1496p-54, 0x1.39e8980a9cc8fp-55, 0x1.2d522ca0c8de2p-54,
        -0x1.e9c23179c2893p-54, -0x1.c93f3b411ad8cp-54, 0x1.dc7f486a4b6bp-54, 0x1.3a1a5bf0d8e43p-54,
        0x1.9d3e12dd8a18bp-54, -0x1.dbb12d006350ap-54, 0x1.74853f3a5931ep-55, 0x1.2eb74966579e7p-57,
    };
    private static final double[] LOG_INV = {
        0x1.0p0, 0x1.fc07f01fc07fp-1, 0x1.f81f81f81f82p-1, 0x1.f44659e4a4271p-1,
        0x1.f07c1f07c1f08p-1, 0x1.ecc07b301eccp-1, 0x1.e9131abf0b767p-1, 0x1.e573ac901e574p-1,
        0x1.e1e1e1e1e1e1ep-1, 0x1.de5d6e3f8868ap-1, 0x1.dae6076b981dbp-1, 0x1.d77b654b82c34p-1,
        0x1.d41d41d41d41dp-1, 0x1.d0cb58f6ec074p-1, 0x1.cd85689039b0bp-1, 0x1.ca4b3055ee191p-1,
        0x1.c71c71c71c71cp-1, 0x1.c3f8f01c3f8fp-1, 0x1.c0e070381c0ep-1, 0x1.bdd2b899406f7p-1,
        0x1.bacf914c1badp-1, 0x1.b7d6c3dda338bp-1, 0x1.b4e81b4e81b4fp-1, 0x1.b2036406c80d9p-1,
        0x1.af286bca1af28p-1, 0x1.ac5701ac5701bp-1, 0x1.a98ef606a63bep-1, 0x1.a6d01a6d01a6dp-1,
        0x1.a41a41a41a41ap-1, 0x1.a16d3f97a4b02p-1, 0x1.9ec8e951033d9p-1, 0x1.9c2d14ee4a102p-1,
        0x1.999999999999ap-1, 0x1.970e4f80cb872p-1, 0x1.948b0fcd6e9ep-1, 0x1.920fb49d0e229p-1,
        0x1.8f9c18f9c18fap-1, 0x1.8d3018d3018d3p-1, 0x1.8acb90f6bf3aap-1, 0x1.886e5f0abb04ap-1,
        0x1.8618618618618p-1, 0x1.83c977ab2beddp-1, 0x1.8181818181818p-1, 0x1.7f405fd017f4p-1,
        0x1.7d05f417d05f4p-1, 0x1.7ad2208e0ecc3p-1, 0x1.78a4c8178a4c8p-1, 0x1.767dce434a9b1p-1,
        0x1.745d1745d1746p-1, 0x1.724287f46debcp-1, 0x1.702e05c0b817p-1, 0x1.6e1f76b4337c7p-1,
        0x1.6c16c16c16c17p-1, 0x1.6a13cd153729p-1, 0x1.6816816816817p-1, 0x1.661ec6a5122f9p-1,
        0x1.642c8590b2164p-1, 0x1.623fa7701624p-1, 0x1.6058160581606p-1, 0x1.5e75bb8d015e7p-1,
        0x1.5c9882b931057p-1, 0x1.5ac056b015acp-1, 0x1.58ed2308158edp-1, 0x1.571ed3c506b3ap-1,
        0x1.5555555555555p-1, 0x1.5390948f40febp-1, 0x1.51d07eae2f815p-1, 0x1.5015015015015p-1,
        0x1.4e5e0a72f0539p-1, 0x1.4cab88725af6ep-1, 0x1.4afd6a052bf5bp-1, 0x1.49539e3b2d067p-1,
        0x1.47ae147ae147bp-1, 0x1.460cbc7f5cf9ap-1, 0x1.446f86562d9fbp-1, 0x1.42d6625d51f87p-1,
        0x1.4141414141414p-1, 0x1.3fb013fb013fbp-1, 0x1.3e22cbce4a902p-1, 0x1.3c995a47babe7p-1,
        0x1.3b13b13b13b14p-1, 0x1.3991c2c187f63p-1, 0x1.3813813813814p-1, 0x1.3698df3de0748p-1,
        0x1.3521cfb2b78c1p-1, 0x1.33ae45b57bcb2p-1, 0x1.323e34a2b10bfp-1, 0x1.30d190130d19p-1,
        0x1.2f684bda12f68p-1, 0x1.2e025c04b8097p-1, 0x1.2c9fb4d812cap-1, 0x1.2b404ad012b4p-1,
        0x1.29e4129e4129ep-1, 0x1.288b01288b013p-1, 0x1.27350b8812735p-1, 0x1.25e22708092f1p-1,
        0x1.2492492492492p-1, 0x1.23456789abcdfp-1, 0x1.21fb78121fb78p-1, 0x1.20b470c67c0d9p-1,
        0x1.1f7047dc11f7p-1, 0x1.1e2ef3b3fb874p-1, 0x1.1cf06ada2811dp-1, 0x1.1bb4a4046ed29p-1,
        0x1.1a7b9611a7b96p-1, 0x1.19453808ca29cp-1, 0x1.1811811811812p-1, 0x1.16e0689427379p-1,
        0x1.15b1e5f75270dp-1, 0x1.1485f0e0acd3bp-1, 0x1.135c81135c811p-1, 0x1.12358e75d3033p-1,
        0x1.1111111111111p-1, 0x1.0fef010fef011p-1, 0x1.0ecf56be69c9p-1, 0x1.0db20a88f4696p-1,
        0x1.0c9714fbcda3bp-1, 0x1.0b7e6ec259dc8p-1, 0x1.0a6810a6810a7p-1, 0x1.0953f39010954p-1,
        0x1.0842108421084p-1, 0x1.073260a47f7c6p-1, 0x1.0624dd2f1a9fcp-1, 0x1.05197f7d73404p-1,
        0x1.041041041041p-1, 0x1.03091b51f5e1ap-1, 0x1.0204081020408p-1, 0x1.010101010101p-1,
    };
    private static final double[] LOG_HI = {
        0x0.0p0, 0x1.fe02a6b106789p-8, 0x1.fc0a8b0fc03e4p-7, 0x1.7b91b07d5b11bp-6,
        0x1.f829b0e7833p-6, 0x1.39e87b9febd6p-5, 0x1.77458f632dcfcp-5, 0x1.b42dd711971bfp-5,
        0x1.f0a30c01162a6p-5, 0x1.16536eea37ae1p-4, 0x1.341d7961bd1d1p-4, 0x1.51b073f06183fp-4,
        0x1.6f0d28ae56b4cp-4, 0x1.8c345d6319b21p-4, 0x1.a926d3a4ad563p-4, 0x1.c5e548f5bc743p-4,
        0x1.e27076e2af2e6p-4, 0x1.fec9131dbeabbp-4, 0x1.0d77e7cd08e59p-3, 0x1.1b72ad52f67ap-3,
        0x1.29552f81ff523p-3, 0x1.371fc201e8f74p-3, 0x1.44d2b6ccb7d1ep-3, 0x1.526e5e3a1b438p-3,
        0x1.5ff3070a793d4p-3, 0x1.6d60fe719d21dp-3, 0x1.7ab890210d909p-3, 0x1.87fa06520c911p-3,
        0x1.9525a9cf456b4p-3, 0x1.a23bc1fe2b563p-3, 0x1.af3c94e80bff3p-3, 0x1.bc286742d8cd6p-3,
        0x1.c8ff7c79a9a22p-3, 0x1.d5c216b4fbb91p-3, 0x1.e27076e2af2e6p-3, 0x1.ef0adcbdc5936p-3,
        0x1.fb9186d5e3e2bp-3, 0x1.0402594b4d041p-2, 0x1.0a324e27390e3p-2, 0x1.1058bf9ae4ad5p-2,
        0x1.1675cababa60ep-2, 0x1.1c898c16999fbp-2, 0x1.22941fbcf7966p-2, 0x1.2895a13de86a3p-2,
        0x1.2e8e2bae11d31p-2, 0x1.347dd9a987d55p-2, 0x1.3a64c556945eap-2, 0x1.404308686a7e4p-2,
        0x1.4618bc21c5ec2p-2, 0x1.4be5f957778a1p-2, 0x1.51aad872df82dp-2, 0x1.5767717455a6cp-2,
        0x1.5d1bdbf5809cap-2, 0x1.62c82f2b9c795p-2, 0x1.686c81e9b14afp-2, 0x1.6e08eaa2ba1e4p-2,
        0x1.739d7f6bbd007p-2, 0x1.792a55fdd47a2p-2, 0x1.7eaf83b82afc3p-2, 0x1.842d1da1e8b17p-2,
        0x1.89a3386c1425bp-2, 0x1.8f11e873662c7p-2, 0x1.947941c2116fbp-2, 0x1.99d958117e08bp-2,
        0x1.9f323ecbf984cp-2, 0x1.a484090e5bb0ap-2, 0x1.a9cec9a9a084ap-2, 0x1.af1293247786bp-2,
        0x1.b44f77bcc8f63p-2, 0x1.b9858969310fbp-2, 0x1.beb4d9da71b7cp-2, 0x1.c3dd7a7cdad4dp-2,
        0x1.c8ff7c79a9a22p-2, 0x1.ce1af0b85f3ebp-2, 0x1.d32fe7e00ebd5p-2, 0x1.d83e7258a2f3ep-2,
        0x1.dd46a04c1c4a1p-2, 0x1.e24881a7c6c26p-2, 0x1.e744261d68788p-2, 0x1.ec399d2468ccp-2,
        0x1.f128f5faf06edp-2, 0x1.f6123fa7028acp-2, 0x1.faf588f78f31fp-2, 0x1.ffd2e0857f498p-2,
        0x1.02552a5a5d0ffp-1, 0x1.04bdf9da926d2p-1, 0x1.0723e5c1cdf4p-1, 0x1.0986f4f573521p-1,
        0x1.0be72e4252a83p-1, 0x1.0e44985d1cc8cp-1, 0x1.109f39e2d4c97p-1, 0x1.12f719593efbcp-1,
        0x1.154c3d2f4d5eap-1, 0x1.179eabbd899a1p-1, 0x1.19ee6b467c96fp-1, 0x1.1c3b81f713c25p-1,
        0x1.1e85f5e7040dp-1, 0x1.20cdcd192ab6ep-1, 0x1.23130d7bebf43p-1, 0x1.2555bce98f7cbp-1,
        0x1.2795e1289b11bp-1, 0x1.29d37fec2b08bp-1, 0x1.2c0e9ed448e8cp-1, 0x1.2e47436e40268p-1,
        0x1.307d7334f10bep-1, 0x1.32b1339121d71p-1, 0x1.34e289d9ce1d3p-1, 0x1.37117b54747b6p-1,
        0x1.393e0d3562a1ap-1, 0x1.3b68449fffc23p-1, 0x1.3d9026a7156fbp-1, 0x1.3fb5b84d16f42p-1,
        0x1.41d8fe84672aep-1, 0x1.43f9fe2f9ce67p-1, 0x1.4618bc21c5ec2p-1, 0x1.48353d1ea88dfp-1,
        0x1.4a4f85db03ebbp-1, 0x1.4c679afccee3ap-1, 0x1.4e7d811b75bb1p-1, 0x1.50913cc01686bp-1,
        0x1.52a2d265bc5abp-1, 0x1.54b2467999498p-1, 0x1.56bf9d5b3f399p-1, 0x1.58cadb5cd7989p-1,
        0x1.5ad404c359f2dp-1, 0x1.5cdb1dc6c1765p-1, 0x1.5ee02a9241675p-1, 0x1.60e32f44788d9p-1,
    };
    private static final double[] LOG_LO = {
        0x0.0p0, -0x1.e44b7e3711ebfp-67, -0x1.83092c59642a1p-62, -0x1.5b602ace3a51p-60,
        0x1.33e3f04f1ef23p-60, -0x1.5bfa937f551bbp-59, 0x1.18d3ca87b9296p-59, -0x1.eb9759c130499p-60,
        0x1.85f325c5bbacdp-59, -0x1.79da3e8c22cdap-60, -0x1.b599f227becbbp-58, 0x1.a49e39a1a8be4p-58,
        -0x1.906d99184b992p-58, -0x1.4a697ab3424a9p-61, 0x1.942f48aa70ea9p-58, 0x1.5d617ef8161b1p-60,
        -0x1.61578001e0162p-60, -0x1.5746b9981b36cp-58, 0x1.9a5dc5e9030acp-57, 0x1.483023472cd74p-58,
        0x1.301771c407dbfp-57, 0x1.de6cb62af18ap-58, 0x1.9f4f6543e1f88p-57, -0x1.746ff8a470d3ap-57,
        -0x1.bc60efafc6f6ep-58, -0x1.caae268ecd179p-57, 0x1.be36b2d6a0608p-59, -0x1.bf7fdbfa08d9ap-57,
        0x1.d904c1d4e2e26p-57, 0x1.93711b07a998cp-59, -0x1.398cff3641985p-58, 0x1.4fce744870f55p-58,
        -0x1.4f689f8434012p-57, 0x1.6e443597e4d4p-57, -0x1.61578001e0162p-59, 0x1.48637950dc20dp-57,
        -0x1.caaae64f21acbp-57, -0x1.28ec217a5022dp-57, 0x1.7dcfde8061c03p-56, 0x1.89fa0ab4cb31dp-58,
        0x1.ce63eab883717p-61, -0x1.0e5c62aff1c44p-60, -0x1.76f5eb09628afp-56, 0x1.7ad24c13f040ep-56,
        -0x1.8f4cdb95ebdf9p-56, -0x1.4dd4c580919f8p-57, -0x1.c68651945f97cp-57, -0x1.0bcfb6082ce6dp-56,
        0x1.f42decdeccf1dp-56, -0x1.259b35b04813dp-57, 0x1.3927ac19f55e3p-59, 0x1.526adb283660cp-56,
        0x1.4236383dc7fe1p-56, 0x1.7b7af915300e5p-57, -0x1.ddea0f7f58e3dp-57, -0x1.cfb1b39ca3a0fp-56,
        -0x1.8c76ceb014b04p-56, 0x1.f057691fe9ed7p-56, 0x1.92ce979ed295p-56, 0x1.24ec519784676p-56,
        -0x1.29639dfbbf0fbp-56, 0x1.f85da755a61a3p-56, -0x1.16cc8bae0bbe4p-56, -0x1.a2b6889dc3e72p-57,
        -0x1.a92e513217f5cp-59, 0x1.5fe535b875a75p-57, -0x1.cadec02b436afp-56, 0x1.133844a15dc28p-58,
        -0x1.cd04495459c78p-56, 0x1.663ec53e23bc4p-56, -0x1.0f3c590a887cap-59, 0x1.cecf052dea69bp-56,
        -0x1.4f689f8434012p-56, 0x1.edf4af2ab4267p-56, 0x1.877b232fafa37p-56, 0x1.41456e8bb2511p-56,
        -0x1.0467656d8b892p-56, 0x1.cbd8f45954a46p-58, -0x1.c825c90c344b9p-58, 0x1.75cee53f35397p-58,
        -0x1.328df13bb38c3p-56, 0x1.8515b0f2db341p-56, -0x1.328260d8abcap-57, 0x1.565f40d9321afp-56,
        -0x1.cb1cb51408cp-56, 0x1.97f304022c9dfp-55, 0x1.395e58e2445bbp-55, -0x1.1b8095ac02f01p-55,
        -0x1.259da11330801p-55, -0x1.22a3442d2d384p-58, -0x1.0e09b27a4373ap-60, 0x1.4c048c671f435p-55,
        -0x1.59c33171a6876p-55, -0x1.00e7c6417e0b4p-55, -0x1.9d1a11443f10cp-56, -0x1.0dac1c4c810e9p-55,
        0x1.ef62cd2f9f1e3p-56, -0x1.b2bf0bc229014p-55, -0x1.f48725e374d6ep-55, 0x1.e021d6d6881e7p-56,
        -0x1.487c0c246978ep-57, -0x1.bd1949a2d1982p-56, -0x1.1a158f3917586p-55, 0x1.0150861a4886bp-55,
        0x1.fb590a1f566dap-57, 0x1.902ab5b3d916bp-56, 0x1.6eb92d885ce4fp-57, -0x1.d117edbdd9103p-56,
        -0x1.58eef67f2483ap-55, -0x1.41c484f9e9b26p-55, -0x1.6fef670bd4b62p-55, 0x1.6d3a754172aefp-55,
        0x1.9192f30bd1806p-55, 0x1.e9c9ee6d83b86p-55, 0x1.f42decdeccf1dp-55, 0x1.cf57a2ecc07f4p-55,
        0x1.13dfa3d3761b6p-60, -0x1.3a5c4c8b39e41p-55, -0x1.8d3d9ea6e9ea9p-55, 0x1.2f2ce96c2d5b1p-55,
        -0x1.1883750ea4d0ap-57, -0x1.5baaf5d2f09f4p-55, 0x1.0471885cd8ff3p-55, 0x1.849792ec98458p-56,
        -0x1.35955683f7196p-59, -0x1.cc2470e8a3df4p-55, 0x1.c358257f49082p-55, -0x1.ac1bb52fa589bp-56,
    };

    private static final double C3 = 1.0 / 3, C4 = -0.25, C5 = 0.2, C6 = -1.0 / 6, C7 = 1.0 / 7, C8 = -0.125;
    private static final double E2 = 0.5, E3 = 1.0 / 6, E4 = 1.0 / 24, E5 = 1.0 / 120, E6 = 1.0 / 720;
    private static final double SPLIT = 134217729.0; // 2^27 + 1, Veltkamp split

    public static double pow(double x, double y) {
        long bits = Double.doubleToRawLongBits(x);
        if (bits < 0x0010000000000000L || bits >= 0x7ff0000000000000L || !(Math.abs(y) < 0x1p30) || y == 0.0) {
            return FdlibmPow.pow(x, y);
        }
        // ln x = hi + lo
        int k = (int) (bits >>> 52) - 1023;
        long mbits = (bits & 0x000fffffffffffffL) | 0x3ff0000000000000L;
        double m = Double.longBitsToDouble(mbits);
        int j = (int) (((mbits >>> 44) & 0xff) + 1) >> 1;          // F = 1 + j/128 nearest to m
        double f, fj;
        if (j == 128) {
            k++;
            j = 0;
            f = (m - 2.0) * 0.5;                                    // exact
            fj = 1.0;
        } else {
            fj = 1.0 + j * 0x1p-7;
            f = m - fj;                                             // exact (Sterbenz)
        }
        double inv = LOG_INV[j], u = f * inv;
        // u + c = f/F: the residual f - u F is exact because u is split so that both products are exact (F has 8 bits)
        double uh = Double.longBitsToDouble(Double.doubleToRawLongBits(u) & 0xfffffffff8000000L), ul = u - uh;
        double c = ((f - uh * fj) - ul * fj) * inv;
        double tail = u * u * (-0.5 + u * (C3 + u * (C4 + u * (C5 + u * (C6 + u * (C7 + u * C8)))))) - u * c;
        double a = k * LN2_HI, h0 = a + LOG_HI[j];                  // k LN2_HI exact
        double e1 = k == 0 ? 0.0 : (a - h0) + LOG_HI[j];            // Fast2Sum, |k ln2| >= ln F when k != 0
        double s = h0 + u, bb = s - h0, e2 = (h0 - (s - bb)) + (u - bb); // TwoSum
        double lo0 = e2 + (e1 + (k * LN2_LO + LOG_LO[j]) + c + tail);
        double hi = s + lo0, lo = lo0 - (hi - s);
        // y ln x = p + pl, p = y hi with its exact rounding error (Dekker product, no fma)
        double p = y * hi;
        if (!(Math.abs(p) <= 708.0)) {
            return FdlibmPow.pow(x, y);                             // overflow, underflow, subnormal results
        }
        double cy = SPLIT * y, yh = cy - (cy - y), yl = y - yh;
        double ch = SPLIT * hi, hh = ch - (ch - hi), hl = hi - hh;
        double pl = (((yh * hh - p) + yh * hl + yl * hh) + yl * hl) + y * lo;
        // e^(p + pl)
        double nd = Math.rint(p * INV_L);
        int n = (int) nd;
        double r = ((p - nd * L_HI) - nd * L_LO) + pl;              // p - nd L_HI exact (nd L_HI exact, Sterbenz)
        double q = r + r * r * (E2 + r * (E3 + r * (E4 + r * (E5 + r * E6))));
        int t = n & 127, e = n >> 7;                                // e in [-1022, 1021]
        double v = EXP_HI[t] + (EXP_LO[t] + EXP_HI[t] * q);
        return v * Double.longBitsToDouble((long) (e + 1023) << 52);
    }

    // For DeterministicMathTest: the tables and constants, to be regenerated and compared bit for bit.
    public static double[][] tables() {
        return new double[][] {EXP_HI.clone(), EXP_LO.clone(), LOG_INV.clone(), LOG_HI.clone(), LOG_LO.clone(),
            {LN2_HI, LN2_LO, L_HI, L_LO, INV_L}};
    }
}
