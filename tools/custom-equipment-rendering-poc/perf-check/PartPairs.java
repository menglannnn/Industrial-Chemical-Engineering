package com.wormzjl.createcheme.science.equipment;

import java.io.*;
import java.util.*;

/** Triangles and faces per part, and for the pairs refused, which check refused them (worst miss per check). */
public final class PartPairs {
    public static void main(String[] args) throws Exception {
        for (var name : new String[]{"ac_fan_ring", "ac_fan", "ac_motor"}) {
            var part = Part.of(name);
            System.out.printf("%s: %d triangles -> %d faces%n", name, part.triangles(), part.faces());
            // Re-read for the refusal reasons over every shared edge.
            List<double[]> v = new ArrayList<>(), vt = new ArrayList<>(), vn = new ArrayList<>();
            List<int[]> idx = new ArrayList<>(), tx = new ArrayList<>(), nx = new ArrayList<>();
            try (var r = new BufferedReader(new InputStreamReader(Part.class.getResourceAsStream("/assets/createcheme/parts/" + name + ".obj")))) {
                for (String line; (line = r.readLine()) != null; ) {
                    var w = line.trim().split("\s+");
                    switch (w[0]) {
                        case "v" -> v.add(new double[]{Double.parseDouble(w[1]), Double.parseDouble(w[2]), Double.parseDouble(w[3])});
                        case "vt" -> vt.add(new double[]{Double.parseDouble(w[1]), 1 - Double.parseDouble(w[2])});
                        case "vn" -> vn.add(new double[]{Double.parseDouble(w[1]), Double.parseDouble(w[2]), Double.parseDouble(w[3])});
                        case "f" -> {
                            int[] a = new int[3], b = new int[3], c = new int[3];
                            for (int k = 0; k < 3; k++) {
                                var s = w[k + 1].split("/");
                                a[k] = Integer.parseInt(s[0]) - 1;
                                b[k] = Integer.parseInt(s[1]) - 1;
                                c[k] = Integer.parseInt(s[2]) - 1;
                            }
                            idx.add(a);
                            tx.add(b);
                            nx.add(c);
                        }
                        default -> {
                        }
                    }
                }
            }
            var edges = new HashMap<Long, Integer>();
            for (int i = 0; i < idx.size(); i++) for (int e = 0; e < 3; e++) edges.put((long) idx.get(i)[e] << 32 | idx.get(i)[(e + 1) % 3], i);
            int pairs = 0;
            double planeMax = 0, uvMax = 0, nMax = 0;
            int planeBad = 0, uvBad = 0, nBad = 0;
            var uvHist = new int[6]; var nHist = new int[6];
            for (int i = 0; i < idx.size(); i++)
                for (int e = 0; e < 3; e++) {
                    var a = idx.get(i);
                    Integer j = edges.get((long) a[(e + 1) % 3] << 32 | a[e]);
                    if (j == null || j < i) continue;
                    pairs++;
                    var bIdx = idx.get(j);
                    int far = 0;
                    for (int c = 0; c < 3; c++) if (bIdx[c] != a[e] && bIdx[c] != a[(e + 1) % 3]) far = c;
                    double[][] p = {v.get(a[0]), v.get(a[1]), v.get(a[2])};
                    double[] wpt = v.get(bIdx[far]);
                    double[] e1 = Mesh.sub(p[1], p[0]), e2 = Mesh.sub(p[2], p[0]), nrm = Mesh.cross(e1, e2), d = Mesh.sub(wpt, p[0]);
                    double area = Math.sqrt(Mesh.dot(nrm, nrm));
                    double off = Math.abs(Mesh.dot(d, nrm)) / area;
                    double A = Mesh.dot(e1, e1), B = Mesh.dot(e1, e2), C = Mesh.dot(e2, e2), det = A * C - B * B;
                    double s = (C * Mesh.dot(d, e1) - B * Mesh.dot(d, e2)) / det, t = (A * Mesh.dot(d, e2) - B * Mesh.dot(d, e1)) / det, rr = 1 - s - t;
                    double uvMiss = 0, nMiss = 0;
                    for (int k = 0; k < 2; k++) {
                        double pred = rr * vt.get(tx.get(i)[0])[k] + s * vt.get(tx.get(i)[1])[k] + t * vt.get(tx.get(i)[2])[k];
                        uvMiss = Math.max(uvMiss, Math.abs(pred - vt.get(tx.get(j)[far])[k]));
                    }
                    for (int k = 0; k < 3; k++) {
                        double pred = rr * vn.get(nx.get(i)[0])[k] + s * vn.get(nx.get(i)[1])[k] + t * vn.get(nx.get(i)[2])[k];
                        nMiss = Math.max(nMiss, Math.abs(pred - vn.get(nx.get(j)[far])[k]));
                    }
                    if (off > Part.EXACT) planeBad++;
                    else if (uvMiss > Part.EXACT) {
                        uvBad++;
                        uvHist[Math.min(5, (int) Math.floor(-Math.log10(uvMiss)))]++;
                    } else if (nMiss > Part.EXACT) { nBad++; nHist[Math.min(5, (int) Math.floor(-Math.log10(nMiss)))]++; }
                    planeMax = Math.max(planeMax, off);
                }
            System.out.printf("  shared edges %d: off-plane %d, uv %d (miss decades 1e-0..1e-5: %s), normal %d (decades %s)%n", pairs, planeBad, uvBad, Arrays.toString(uvHist), nBad, Arrays.toString(nHist));
        }
    }
}
