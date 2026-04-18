package dev.simon;
import java.nio.file.Path;
import java.util.*;
import java.io.File;

import dev.simon.model.BackgroundPair;
import dev.simon.model.DiffPair;
import dev.simon.model.ProjectPair;

public class Main {
    public static void main(String[] args) {
        String workingDir = FissionLessPathUtil.getWorkingDirByEnvironmentVariable("/tmp/");
        Montage montage = new Montage(workingDir);
        // mProjectPPs
        List<String> inputFits = List.of("input/2mass-atlas-001020s-k0860033.fits","input/2mass-atlas-001020s-k0860044.fits", "input/2mass-atlas-001020s-k0860056.fits","input/2mass-atlas-001020s-k0870221.fits","input/2mass-atlas-001020s-k0870233.fits","input/2mass-atlas-001020s-k0870245.fits","input/2mass-atlas-001021s-k0490221.fits","input/2mass-atlas-001021s-k0490233.fits","input/2mass-atlas-001021s-k0490245.fits","input/2mass-atlas-001021s-k0560033.fits","input/2mass-atlas-001021s-k0560044.fits","input/2mass-atlas-001021s-k0560056.fits","input/2mass-atlas-001021s-k0570221.fits","input/2mass-atlas-001021s-k0570233.fits","input/2mass-atlas-001021s-k0570245.fits","input/2mass-atlas-980914s-k0800033.fits","input/2mass-atlas-980914s-k0800044.fits","input/2mass-atlas-980914s-k0800056.fits","input/2mass-atlas-980914s-k0810221.fits","input/2mass-atlas-980914s-k0810233.fits","input/2mass-atlas-980914s-k0810245.fits","input/2mass-atlas-980914s-k0820033.fits","input/2mass-atlas-980914s-k0820044.fits","input/2mass-atlas-980914s-k0820056.fits","input/2mass-atlas-980914s-k0830221.fits","input/2mass-atlas-980914s-k0830233.fits","input/2mass-atlas-980914s-k0830245.fits","input/2mass-atlas-980914s-k0840033.fits","input/2mass-atlas-980914s-k0840044.fits","input/2mass-atlas-980914s-k0840056.fits");
        String regionHdr = "region.hdr";
        String inputDir = "input/";
        List<ProjectPair> projectPairs = inputFits.stream().map(str -> new ProjectPair(str, "p" + str.substring(6))).toList();
        for(ProjectPair pair : projectPairs) {
            montage.mProjectPP(List.of("-X"), pair, regionHdr);
        }

        // prepareMDiffFit
        // mImgTbl
        String imagesTbl = "images.tbl";
        String imageList = Path.of(workingDir, "input.imglist").toString();
        montage.mImgtbl(List.of("-t" + imageList), inputDir, imagesTbl);

        // mDAGTbls
        String rImagesTbl = "rimages.tbl";
        String pImagesTbl = "pimages.tbl";
        String cImagesTbl = "cimages.tbl";
        montage.mDAGTbls(imagesTbl, regionHdr, rImagesTbl, pImagesTbl, cImagesTbl);

        // mOverlaps
        String diffsTbl = "diffs.tbl";
        montage.mOverlaps(List.of(), pImagesTbl, diffsTbl);

        // mDiffFit
        // create DiffPairs
        List<DiffPair> diffPairs = montage.createDiffPairs(diffsTbl);

        // mDiff
        for(DiffPair pair : diffPairs) {
            montage.mDiff(List.of(), pair, regionHdr);
        }

        for (DiffPair pair : diffPairs) {
            montage.mFitplane(List.of(), pair);
        }
        
        montage.mStatFile();

        // mConcatFit
        String statFile = "statfile.tbl";
        String fitsTbl = "fits.tbl";
        montage.mConcatFit(statFile, fitsTbl);

        
        // mBgModel
        String correctionsTbl = "corrections.tbl";
        montage.mBgModel(List.of(), pImagesTbl, fitsTbl, correctionsTbl);
        
        // mBackground
        // creates the names for the corrected fits by adding "_corrected.fits" to the end of the projected fits
        String correctedDir = "corrected/";
        if (!new File(montage.getAbsolutePath(correctedDir)).exists()){
            if (!new File(montage.getAbsolutePath(correctedDir)).mkdir()){
                throw new RuntimeException("Could not create directory: " + montage.getAbsolutePath(correctedDir));
            }
        }
        String modPImagesTbl = "modpimages.tbl";
        montage.setAbsolutePathInImagesTbl(pImagesTbl, modPImagesTbl, "p2mass");
        
        List<BackgroundPair> backgroundPairs = inputFits.stream().map(str -> new BackgroundPair("p" + str.substring(6), correctedDir + "p" + str.substring(6, str.length() -5) + "_corrected.fits")).toList();
        for (BackgroundPair pair : backgroundPairs) {
            montage.mBackground(List.of("-t"), pair, modPImagesTbl, correctionsTbl);
        }

        // mImgTbl
        montage.mImgtbl(List.of(), correctedDir, cImagesTbl);

        // mAdd
        String mosaicFits = "mosaic.fits";
        montage.mAdd(List.of(), cImagesTbl, regionHdr, mosaicFits);

        // mShrink
        String mosaicShrinked = "mosaic_shrinked.fits";
        montage.mShrink(List.of(), mosaicFits, mosaicShrinked, 1.5f);

        // mViewer
        String finalImage = "final.png";
        montage.mViewer(List.of("-ct 1", "-gray"), mosaicShrinked, List.of("-2s max gaussian-log", "-out"),  finalImage);
    }
}