package artisynth.demos.assignment;

import java.awt.Color;
import java.io.IOException;
import java.util.*;

import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.materials.LinearMaterial;

import maspack.geometry.*;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

public class VocalFolds extends RootModel {
   private MechModel mech;
   
   private static final double GLOTTAL_WIDTH = 3.0; // Arndt & Schafer (1994)  (umlaut "a" in Schafer)
   
   private static final boolean SHOW_RENDERING = false;
   
   
   
   private VocalFoldBuilder vfBuilder = new VocalFoldBuilder();
   private double vocalFoldTransverseWidth = VocalFoldBuilder.getWidth();
   
   private double EPS = 1e-7;
   private ArrayList<Point3d> leftMedialOrigPos = new ArrayList<Point3d>();
   private ArrayList<Point3d> rightMedialOrigPos = new ArrayList<Point3d>();
   
   
   private void setRenderProps(FemModel3d fem) {
      fem.setSurfaceRendering (SurfaceRender.Shaded);
//      RenderProps.setLineColor (fem, Color.DARK_GRAY);
      if (!SHOW_RENDERING)
         RenderProps.setLineWidth (fem, 0);
      RenderProps.setLineColor (fem, new Color (249/255f, 212/255f, 160/255f));
      RenderProps.setFaceColor (fem, new Color (249/255f, 212/255f, 160/255f));
//      RenderProps.setFaceColor (fem, Color.WHITE);
   }
   
   
   public void build (String[] args) throws IOException {
      super.build (args);
      
      mech = new MechModel ("mech");
      mech.setGravity (0, -9.81, 0);
      addModel(mech);
      
      // Before x-translation, both folds form a 'semicircular' cylinder
      // Therefore we only have to fix points on both FEMs that lie near 
      // the plane where x=0
      boolean isRight = true;
      FemModel3d vofoLeft = vfBuilder.buildFem(!isRight);
      fixateNodes(vofoLeft);
      ArrayList<FemNode3d> leftMedialNodes = getMedialNodes(vofoLeft, !isRight);
      
      
      FemModel3d vofoRight = vfBuilder.buildFem(isRight);
      fixateNodes(vofoRight);
      ArrayList<FemNode3d> rightMedialNodes = getMedialNodes(vofoRight, isRight);
      
      // Move each FEM to correct pos along x-axis
      double transposeX = vocalFoldTransverseWidth - 0.38;// + GLOTTAL_WIDTH * 0.5;
      vofoLeft.transformGeometry (new RigidTransform3d(-transposeX, 0, 0));
      vofoRight.transformGeometry (new RigidTransform3d(transposeX, 0, 0));
      
      VocalFoldsController vofoCon = new VocalFoldsController(
         leftMedialNodes,
         leftMedialOrigPos,
         rightMedialNodes,
         rightMedialOrigPos);
   
      vofoCon.setModel (mech);
      vofoCon.setActive (true);
      addController(vofoCon);

      mech.addModel (vofoLeft);
      mech.addModel (vofoRight);
      
//      mech.setCollisionBehavior (vofoLeft, vofoRight, new CollisionBehavior (true, 0));
      
      setRenderProps (vofoLeft);
      setRenderProps (vofoRight);
      
   }
   
   
   private boolean isNearZero(double d) { return Math.abs(d) <= EPS; }
   
   
   private void fixateNodes(FemModel3d fem) {
      for ( FemNode3d n : fem.getNodes() ) {
         Point3d pos = n.getPosition();
         if ( isNearZero (pos.x) )
            n.setDynamic (false);
      }
   }
   
   
   /**
    * Runs before x-transposing either half
    * @param fem
    * @param isRight
    * @return ArrayList of medial nodes
    */
   private ArrayList<FemNode3d> getMedialNodes(FemModel3d fem, boolean isRight) {
      double medialOffsetX = 0;
      int flipSign = isRight ? -1 : 1;
      
      ArrayList<FemNode3d> medialNodes = new ArrayList<FemNode3d>();
      
      // First iteration: find medial offset
      for ( FemNode3d n : fem.getNodes() ) {
         Point3d pos = n.getPosition();
//         if (isRight)
//            System.out.printf("%.2f : %.2f > %.2f %b\n", pos.x * flipSign, pos.x * flipSign, medialOffsetX * flipSign, (pos.x * flipSign > medialOffsetX * flipSign));
         if ( pos.x * flipSign > medialOffsetX * flipSign )
            medialOffsetX = pos.x;
      }
      
      System.out.printf ("medialOffsetX for %s vofo: %.2f\n",
            isRight ? "right" : "left",
            medialOffsetX);
      
      // Second iteration: find medial-most nodes
      for ( FemNode3d n : fem.getNodes() ) {
         double posX = n.getPosition().x;
         boolean isMedial = Math.abs(posX - medialOffsetX) < .04;
         if ( isMedial ) {
            medialNodes.add (n);
         }
      }
      
      // Do stuff with medial nodes here
      for ( FemNode3d n : medialNodes ) {
         Point3d pos = n.getPosition();
         
         // Visually mark each node
         if (SHOW_RENDERING) {
            FemMarker mkr = new FemMarker (/*name=*/null, pos.x, pos.y, pos.z);
            RenderProps.setSphericalPoints (mkr, 0.2, Color.BLUE);
            fem.addMarker (mkr);
         }
         
         // Cache initial locations
         if (isRight) {
            rightMedialOrigPos.add (pos);
         } else {
            leftMedialOrigPos.add (pos);
         }
         
         // Set non-dynamic to ignore forces
         n.setDynamic (false);
      }
      return medialNodes;
   }
   
   
   
}
