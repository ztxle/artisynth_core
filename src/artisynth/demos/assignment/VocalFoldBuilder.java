package artisynth.demos.assignment;

import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.materials.LinearMaterial;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Vector3d;
import maspack.matrix.RigidTransform3d;

public class VocalFoldBuilder {
   // Following values from Deguchi, Kawahara, Takahashi (2010)
   // 16kPa for membrane, 12kPa for ligament, 8 kPa for TA muscle
   private static double youngsModulus = 12000;
   private static double poissonRatio = 0.3;
   
   // Dimensions in millimeters
   private static double vocalFoldSaggitalWidth = 14.0 * 1.0; // scale down to speedup sim
   private static double vocalFoldTransverseWidth = 7.7;
   
   private boolean isRightSide = false;
   
   
   public static double getWidth() { return vocalFoldTransverseWidth; }
   
   
   private PolygonalMesh createVocalFoldMesh(boolean isRightSide) {
      double cylHeight_z = vocalFoldSaggitalWidth;
      double cylBigRad_x = vocalFoldTransverseWidth;
      double cylSmallRad_x = 2.0;
      
      double flipBit = isRightSide ? -1.0 : 1.0;
      
      PolygonalMesh cylBig = MeshFactory.createCylinder (cylBigRad_x, cylHeight_z, 50);
      
      PolygonalMesh boxCrop = MeshFactory.createBox (
         /*dim xyz=*/   7.5, 7.7, vocalFoldSaggitalWidth, 
         /*off xyz=*/   7.5/2*flipBit, (-7.7)/2.0-2.8, 0);
      PolygonalMesh quartCyl = MeshFactory.getIntersection (cylBig, boxCrop);
              
      PolygonalMesh boxFlat = MeshFactory.createBox (
         /*dim xyz=*/   5.3, 2.8, vocalFoldSaggitalWidth,
         /*off xyz=*/   5.3/2.0*flipBit, -2.8/2.0-0.01, 0);
      
      PolygonalMesh quartFlat = MeshFactory.getUnion (quartCyl, boxFlat);

      PolygonalMesh cylSmall = MeshFactory.createCylinder (cylSmallRad_x, cylHeight_z, 50);
      Vector3d moveCylSmall = new Vector3d(5.3*flipBit, -2.0-0.01, 0);
      cylSmall.translate (moveCylSmall);

      PolygonalMesh vofo = MeshFactory.getUnion (quartFlat, cylSmall);
      
      return vofo;
   }
   
   
//   private PolygonalMesh createTaMuscleMesh(boolean isRightSide) {
//      double membraneLigament = 0.35 + 0.8;
//      double cylHeight_z = vocalFoldSaggitalWidth;
//      double cylBigRad_x = vocalFoldTransverseWidth - membraneLigament;
//      double cylSmallRad_x = (2.0-membraneLigament)*0.5+0.1;
//      
//      double flipBit = isRightSide ? -1.0 : 1.0;
//      
//      PolygonalMesh cylBig = MeshFactory.createCylinder (cylBigRad_x, cylHeight_z, 50);
//      
//      PolygonalMesh boxCrop = MeshFactory.createBox (
//         /*dim xyz=*/   7.5-membraneLigament, 7.7, cylHeight_z, 
//         /*off xyz=*/   (7.5-membraneLigament)/2*flipBit, (-7.7)/2.0-2.8, 0);
//      PolygonalMesh quartCyl = MeshFactory.getIntersection (cylBig, boxCrop);
//
//      PolygonalMesh boxFlat = MeshFactory.createBox (
//         /*dim xyz=*/   5.3-0, 2.8-membraneLigament*2, cylHeight_z,
//         /*off xyz=*/   (5.3-0)/2.0*flipBit, (-2.8-membraneLigament*2)/2.0-0.01, 0);
//      
//      PolygonalMesh quartFlat = MeshFactory.getUnion (quartCyl, boxFlat);
//      
//      PolygonalMesh cylSmall = MeshFactory.createCylinder (cylSmallRad_x, cylHeight_z, 50);
//      Vector3d moveCylSmall = new Vector3d((5.3+0.082)*flipBit, -membraneLigament+0.3-1.98, 0);
//      cylSmall.translate (moveCylSmall);
//
//      PolygonalMesh vofo = MeshFactory.getUnion (quartFlat, cylSmall);
//      
//      return vofo;
//   }

   
   
   private RigidBody buildForceEffectorBar() {
      double radius = 1.0;
      double length = vocalFoldSaggitalWidth;
      double density = 0.0;
      RigidBody bar = RigidBody.createCylinder ("forceEffectorBar", radius, length, density, 3);
      return bar;
   }
   
   
   public FemModel3d buildFem(boolean buildRightSide) {
      this.isRightSide = buildRightSide;
      
      // Generate VoFo FEM from mesh
      String femName = String.format ("vocalFold%sFem", this.isRightSide ? "Right" : "");
      FemModel3d fem = new FemModel3d(femName);
      PolygonalMesh vofoMesh = createVocalFoldMesh(buildRightSide);
      fem = FemFactory.createFromMesh (fem, vofoMesh, /*quality=*/200);

      // FEM material props
      fem.setDensity (10);
      fem.setParticleDamping (0.1);
      fem.setMaterial (new LinearMaterial(youngsModulus, poissonRatio));
      
      return fem;
   }
   
   
//   public FemModel3d buildTaMuscle(boolean buildRightSide) {
//      this.isRightSide = buildRightSide;
//      // Generate "outer" and "outer" mesh
//      double scale = 1 - (0.35 + 0.8) / vocalFoldTransverseWidth; 
//      PolygonalMesh mesh = createTaMuscleMesh(buildRightSide);
//      
//      // Generate FEM
//      String femName = String.format ("thyroarytenoid%sFem", this.isRightSide ? "Right" : "");
//      FemModel3d fem = new FemModel3d(femName);
//      fem = FemFactory.createFromMesh (fem, mesh, 200);
//      
//      // FEM material props
//      fem.setDensity (10);
//      fem.setParticleDamping (0.1);
//      fem.setMaterial (new LinearMaterial(youngsModulus, poissonRatio));
//      
//      return fem; 
//   }
   
   public FemModel3d buildFem() { return buildFem(false); }
}
