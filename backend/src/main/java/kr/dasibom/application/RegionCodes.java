package kr.dasibom.application;
import java.util.*;

/** Explicit one-to-one code aliases observed in the 2026 public visitor and boundary data.
 * Merged/split Incheon districts are deliberately not guessed. Existing service URLs stay stable. */
public final class RegionCodes {
    private RegionCodes() {}
    private static final Map<String,String> CURRENT=Map.ofEntries(
        Map.entry("29110","12210"),Map.entry("29140","12240"),Map.entry("29155","12270"),Map.entry("29170","12300"),Map.entry("29200","12330"),
        Map.entry("46110","12110"),Map.entry("46130","12130"),Map.entry("46150","12150"),Map.entry("46170","12170"),Map.entry("46230","12190"),
        Map.entry("46710","12710"),Map.entry("46720","12720"),Map.entry("46730","12730"),Map.entry("46770","12740"),Map.entry("46780","12750"),Map.entry("46790","12760"),
        Map.entry("46800","12770"),Map.entry("46810","12780"),Map.entry("46820","12790"),Map.entry("46830","12800"),Map.entry("46840","12810"),Map.entry("46860","12820"),
        Map.entry("46870","12830"),Map.entry("46880","12840"),Map.entry("46890","12850"),Map.entry("46900","12860"),Map.entry("46910","12870")
    );
    public static String currentCode(String serviceCode){return CURRENT.getOrDefault(serviceCode,serviceCode);}
    public static String serviceCode(String currentCode){return CURRENT.entrySet().stream().filter(e->e.getValue().equals(currentCode)).map(Map.Entry::getKey).findFirst().orElse(currentCode);}
}
