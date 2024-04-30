package dev.simon;

import dev.simon.model.BackgroundPair;
import dev.simon.model.DiffPair;
import dev.simon.model.ProjectPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        String workingDir = FissionLessPathUtil.getWorkingDirByEnvironmentVariable("/home/ec2-user/function/");
        String storageUri = "s3://file-bucket-eu-central-1-f0deb035";
        Montage montage = new Montage(workingDir);
        DynamicJStorage dynamicJStorage = new DynamicJStorage(workingDir, storageUri);

        List<String> inputFits = List.of("input/2mass-atlas-001020s-k0860033.fits", "input/2mass-atlas-001020s-k0860044.fits", "input/2mass-atlas-001020s-k0860056.fits", "input/2mass-atlas-001020s-k0870221.fits", "input/2mass-atlas-001020s-k0870233.fits", "input/2mass-atlas-001020s-k0870245.fits", "input/2mass-atlas-001021s-k0490221.fits", "input/2mass-atlas-001021s-k0490233.fits", "input/2mass-atlas-001021s-k0490245.fits", "input/2mass-atlas-001021s-k0560033.fits", "input/2mass-atlas-001021s-k0560044.fits", "input/2mass-atlas-001021s-k0560056.fits", "input/2mass-atlas-001021s-k0570221.fits", "input/2mass-atlas-001021s-k0570233.fits", "input/2mass-atlas-001021s-k0570245.fits", "input/2mass-atlas-980914s-k0800033.fits", "input/2mass-atlas-980914s-k0800044.fits", "input/2mass-atlas-980914s-k0800056.fits", "input/2mass-atlas-980914s-k0810221.fits", "input/2mass-atlas-980914s-k0810233.fits", "input/2mass-atlas-980914s-k0810245.fits", "input/2mass-atlas-980914s-k0820033.fits", "input/2mass-atlas-980914s-k0820044.fits", "input/2mass-atlas-980914s-k0820056.fits", "input/2mass-atlas-980914s-k0830221.fits", "input/2mass-atlas-980914s-k0830233.fits", "input/2mass-atlas-980914s-k0830245.fits", "input/2mass-atlas-980914s-k0840033.fits", "input/2mass-atlas-980914s-k0840044.fits", "input/2mass-atlas-980914s-k0840056.fits");

        // DEFINE ALL FILES THAT ARE NEEDED FOR THE LOCAL EXECUTION
        List<String> initialDownloadFiles = new ArrayList<>(List.of("mImgtbl", "mDAGTbls", "mOverlaps", "mStatFile", "mConcatFit", "mBgModel", "mAdd", "mShrink", "mViewer", "region.hdr", "input.imglist", "fits.txt"));
        initialDownloadFiles.addAll(inputFits);
        // DOWNLOAD ALL FILES
        initialDownloadFiles.forEach(dynamicJStorage::ensureLocal);


        // mProjectPPs
        String regionHdr = "region.hdr";
        String inputDir = "input/";
        List<ProjectPair> projectPairs = inputFits.stream().map(str -> new ProjectPair(str, "p" + str.substring(6))).toList();

        // INVOKE mProjectPPs

        // prepareMDiffFit
        // mImgTbl
        String imagesTbl = "images.tbl";
        String imageList = "input.imglist";
        montage.mImgtbl(List.of("-t"), imageList, inputDir, imagesTbl, inputFits);

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

        // INVOKE mDiffFit

        // mStatFile
        montage.mStatFile();

        // mConcatFit
        String statFile = "statfile.tbl";
        String fitsTbl = "fits.tbl";
        montage.mConcatFit(statFile, fitsTbl, diffPairs);


        // mBgModel
        String correctionsTbl = "corrections.tbl";
        montage.mBgModel(List.of(), pImagesTbl, fitsTbl, correctionsTbl);

        // mBackground
        // creates the names for the corrected fits by adding "_corrected.fits" to the end of the projected fits
        String correctedDir = "corrected/";
        String modPImagesTbl = "modpimages.tbl";
        montage.setAbsolutePathInImagesTbl(pImagesTbl, modPImagesTbl, "p2mass");
        List<BackgroundPair> backgroundPairs = inputFits.stream().map(str -> new BackgroundPair("p" + str.substring(6), correctedDir + "p" + str.substring(6, str.length() - 5) + "_corrected.fits")).toList();

        List<String> uploadBeforeMBackground = List.of("corrections.tbl", "modpimages.tbl");
        uploadBeforeMBackground.forEach(dynamicJStorage::ensureStorage);
       
        // INVOKE mBackgrounds

        // DEFINE ALL FILES AFTER mBackground
        List<String> downloadListAftermBackground = List.of( "corrected/p2mass-atlas-001020s-k0870221_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0840044_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0840044_corrected_area.fits",
                "corrected/p2mass-atlas-001021s-k0570221_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0820044_corrected.fits",
                "corrected/p2mass-atlas-001021s-k0570245_corrected.fits",
                "corrected/p2mass-atlas-001021s-k0560056_corrected_area.fits",
                "corrected/p2mass-atlas-001021s-k0490233_corrected_area.fits",
                "corrected/p2mass-atlas-001021s-k0490245_corrected.fits",
                "corrected/p2mass-atlas-001021s-k0570233_corrected_area.fits",
                "corrected/p2mass-atlas-001020s-k0870233_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0810245_corrected.fits",
                "corrected/p2mass-atlas-001021s-k0490221_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0830221_corrected.fits",
                "corrected/p2mass-atlas-001020s-k0860033_corrected_area.fits",
                "corrected/p2mass-atlas-001020s-k0860056_corrected.fits",
                "corrected/p2mass-atlas-001020s-k0860056_corrected_area.fits",
                "corrected/p2mass-atlas-001021s-k0570233_corrected.fits",
                "corrected/p2mass-atlas-001021s-k0560044_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0820056_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0810233_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0830233_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0820044_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0840033_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0820033_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0800033_corrected.fits",
                "corrected/p2mass-atlas-001020s-k0860033_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0820056_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0840056_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0810221_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0830245_corrected_area.fits",
                "corrected/p2mass-atlas-001020s-k0860044_corrected_area.fits",
                "corrected/p2mass-atlas-001021s-k0490221_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0820033_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0830233_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0840033_corrected.fits",
                "corrected/p2mass-atlas-001021s-k0560044_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0800044_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0800056_corrected.fits",
                "corrected/p2mass-atlas-001020s-k0870221_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0800033_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0810245_corrected_area.fits",
                "corrected/p2mass-atlas-001020s-k0870245_corrected.fits",
                "corrected/p2mass-atlas-001020s-k0870233_corrected_area.fits",
                "corrected/p2mass-atlas-001021s-k0560033_corrected_area.fits",
                "corrected/p2mass-atlas-001021s-k0560056_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0830245_corrected.fits",
                "corrected/p2mass-atlas-980914s-k0830221_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0810221_corrected.fits",
                "corrected/p2mass-atlas-001021s-k0570221_corrected.fits",
                "corrected/p2mass-atlas-001021s-k0490245_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0800056_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0800044_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0810233_corrected_area.fits",
                "corrected/p2mass-atlas-001021s-k0490233_corrected.fits",
                "corrected/p2mass-atlas-001021s-k0570245_corrected_area.fits",
                "corrected/p2mass-atlas-980914s-k0840056_corrected.fits",
                "corrected/p2mass-atlas-001021s-k0560033_corrected.fits",
                "corrected/p2mass-atlas-001020s-k0870245_corrected_area.fits",
                "corrected/p2mass-atlas-001020s-k0860044_corrected.fits");

        // DOWNLOAD ALL FILES AFTER mBackground
        downloadListAftermBackground.forEach(dynamicJStorage::ensureLocal);

        // mImgTbl
        List<String> correctedFits = Stream.concat(backgroundPairs.stream().map(BackgroundPair::getCorrectedFit), backgroundPairs.stream().map(BackgroundPair::getCorrectedAreaFit)).toList();
        montage.mImgtbl(List.of(), correctedDir, cImagesTbl, correctedFits);

        // mAdd
        String mosaicFits = "mosaic.fits";
        montage.mAdd(List.of(), cImagesTbl, regionHdr, correctedFits, mosaicFits);

        // mShrink
        String mosaicShrinked = "mosaic_shrinked.fits";
        montage.mShrink(List.of(), mosaicFits, mosaicShrinked, 1.5f);

        // mViewer
        String finalImage = "final.png";
        montage.mViewer(List.of("-ct 1", "-gray"), mosaicShrinked, List.of("-2s max gaussian-log", "-out"), finalImage);
    }
}