//Name: Nicholas Ng Nuo Yang
//Mat No: A0112224B
//program description: Simulating Physical Memory and translation of Virtual Addresses




import java.util.*;
import java.io.*;
import java.lang.*;
import java.math.*;

class VirMem{
 //declare data structures where all classes can use.
 int[] PhyMem;
 int[] BitMapFrames;
 int [][] TLB;
 FileWriter fw;
 public VirMem(){}
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 void InitializePM (InputStream in){
  PhyMem = new int[524288];
  BitMapFrames = new int[32];
  Scanner sc = new Scanner(in);
  String strs_f = sc.nextLine();
  String [] s_f = strs_f.split(" ");
  BitMapFrames[0] = BitMapFrames[0] | (1<<31) ; //segment table occupies frame 0. mark frame 0 as occupied,
  for (int i = 0 ; i < s_f.length; i = i + 2){    //entries into segment table
   int s = Integer.parseInt(s_f [0 + i]);
   int f = Integer.parseInt(s_f [1 + i]);
   entrytoST(s, f);
  }
  String strp_s_f = sc.nextLine();
  String []p_s_f = strp_s_f.split(" ");
 
  for (int i = 0 ; i < p_s_f.length; i = i + 3){  // entries into page table
   int p = Integer.parseInt(p_s_f [0 + i]);
   int s = Integer.parseInt(p_s_f [1 + i]);
   int f = Integer.parseInt(p_s_f [2 + i]);
   entrytoPT(p, s, f);
  }
 }

 void entrytoST(int s, int f){ // PT of segment s starts at address f (if f == -1 , PT not resident in PM)
  PhyMem[0+s] = f;
  if (f != -1 && f != 0){                   //PT must be in mem && must exist ==> indicate in bitmap that frame is occupied
   int frameno = f/512 ;
   int frameno2 = frameno + 1;           //PT occupies 2 frames
   int integer_inBitMap = frameno / 32;
   int integer_inBitMap2 = frameno2 / 32;
   int nst_bit = frameno % 32;
   int nst_bit2 = frameno2 % 32;
   int shift = 31 - nst_bit;  
   int shift2 = 31 - nst_bit2;                                           
   BitMapFrames[integer_inBitMap] = BitMapFrames[integer_inBitMap] | (1 << shift) ;
   BitMapFrames[integer_inBitMap2] = BitMapFrames[integer_inBitMap2] | (1 << shift2) ;
  }
 }

 void entrytoPT(int p, int s, int f){ //page p of segment s starts at address f ( if f == -1, page not resident in PM)
  int segadd = PhyMem[s];        // access page table of segment
  int pageadd = segadd + p;      // go to corresponding position for p in page table
  PhyMem[pageadd] = f;           // indicate that page starts at f.
  if (f != -1 && f != 0){                   //Page must be in mem && must exist ==> indicate in bitmap that frame is occupied
   int frameno = f/512 ;
   int integer_inBitMap = frameno / 32;
   int nst_bit = frameno % 32;
   int shift = 31 - nst_bit;                                            
   BitMapFrames[integer_inBitMap] = BitMapFrames[integer_inBitMap] | (1 << shift) ; //page data occupies 1 frame
  }
 }

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 void translate (InputStream in) throws IOException{
  TLB = new int[4][3];
  for (int i = 0; i < 4; i ++){      // initiate TLB with no entries
    for (int j = 1;  j < 3; j++){
      TLB[i][j] = -1;
    } 
  }

  Scanner trans = new Scanner (in);
  String strop_VA = trans.nextLine();
  String []op_VA = strop_VA.split(" ");
  for (int i = 0 ; i < op_VA.length; i = i + 2){ 
   System.out.println("---get VA to translate---");
   int op = Integer.parseInt(op_VA [0 + i]);
   int VirAdd = Integer.parseInt(op_VA [1 + i]);
   System.out.println("translating VA");
   va_2_pa_TLB(op, VirAdd);
  }
  System.out.println("END-- NO MORE VA to translate");
 }

 

 void va_2_pa_TLB (int mode, int va) throws IOException {
  
  //obtain sp and w from the corresponding va
  int mask_sp = createMask(4, 22); 
  int sp = (mask_sp & va) >> 9;
  int mask_w = createMask(23, 31);
  int w = (mask_w & va);
  int a = 0;
   //check if sp exists within TLB
   for( int i =0 ; i < 4 ; i++){
    if (TLB[i][1] == sp){
      a=1;
      int address = TLB[i][2] + w;
      fw.write("h "+ address + " ");
      decrementLRU(i);
      break;
    }
   }

  if (a == 0 ){ // no match is found TLB
    System.out.println("no match in TLB");
    Std_VA_translate(mode, sp, w);
  }
 }

 void Std_VA_translate(int mode, int sp, int w) throws IOException{
    int mask_s = createMask(13,21);
    int mask_p = createMask(22,31);
    int s = (mask_s & sp) >> 10;
    int p = (mask_p & sp);
   
    if (mode == 0){    //read
      if(PhyMem[s] == -1 || PhyMem[PhyMem[s] + p] == -1){
        fw.write("pf ");
      }else if(PhyMem[s] == 0 || PhyMem[PhyMem[s] + p] == 0){
        System.out.println("error");
        fw.write("error ");
      }else{
        int pa = PhyMem[PhyMem[s] + p] + w;
        updateTLB(sp, s , p);
        System.out.println("m " + pa + " ");
        fw.write("m " + pa + " ");
      }
    }else{//write
      if(PhyMem[s] == -1 || PhyMem[PhyMem[s] + p] == -1){
        fw.write("pf ");
      }else{
        if(PhyMem[s] == 0){ //ST entry is 0
          PhyMem[s] = checkbitmap(0); // update address pointing to PT for the Segment;
        }
        if (PhyMem[PhyMem[s] + p] == 0){ //PT entry is 0
          PhyMem[PhyMem[s] + p]  = checkbitmap(1); //allocate new blank page ;
        }
        int pa = PhyMem[PhyMem[s] + p] + w;
        updateTLB(sp, s , p);
        System.out.println("m " + pa + " ");
        fw.write("m " + pa + " ");
      }
    }
 }

 int checkbitmap(int a){
  if ( a == 0){  //ST entry is 0 , find two consecutive empty frame
    int PTadd = 0;
    for (int i = 0 ; i < 32; i++){ //scan trhough the bitmap represented by 32 integers
      if(BitMapFrames[i] != -1 ){ // there is free frame in this integer
        int b = BitMapFrames[i];
        int bnext = BitMapFrames[i+1];
        String k = String.format("%32s", Integer.toBinaryString(b)).replace(' ', '0');
        String knext = String.format("%32s", Integer.toBinaryString(bnext)).replace(' ', '0');
        int c = k.indexOf("00"); //position in integer of the 2 free frames
        int l = k.lastIndexOf("0");
        int first = knext.indexOf("0");
        if (c != -1){//found the suitable frame
          int shift = 30 - c;
          BitMapFrames[i] = BitMapFrames[i] | (3 << shift); //update bit map
          PTadd = (c + (i * 32)) * 512;
          break;                    //PT starting address
        }
        if (l == 31 && first == 0){ //consecutive frames in different integers
          BitMapFrames[i] = BitMapFrames[i] | 1;           //update bitmap
          BitMapFrames[i+1] = BitMapFrames[i+1] | (1<<31);
          PTadd = (31 + (i * 32))*  512;   // PT starting address
          break;
        }
      }
    }
    return PTadd;
  }
  else { //PTentry is 0, find an empty frame;
    int Padd = 0;
    for (int j = 0;  j < 32; j ++){ //scan through the bitmap represented by 32 integers
      if (BitMapFrames[j] != -1){ //there is a free frame in this integer rep.
        int z = BitMapFrames[j];
        String zstr = String.format("%32s", Integer.toBinaryString(z)).replace(' ', '0');
        int y = zstr.indexOf("0"); // position in integer of the free frame
        int shift = 31 - y;
        BitMapFrames[j] = BitMapFrames[j] | (1 << shift); //update bitmap
        Padd = (y + j*32) * 512;
        break;
      }
    }
    return Padd;
  } 
}

 void updateTLB(int sp, int s, int p){
    for (int i = 0; i < 4; i ++){                
      if (TLB[i][0] == 0 | TLB[i][0] == -1 ){
        TLB[i][1] = sp;
        TLB[i][2] = PhyMem[PhyMem[s] + p];
        for (int j = 0; j < 4; j++){
          if (TLB[j][0] != -1 && TLB[j][0] != 0)
            TLB[j][0]--;
        }
        TLB[i][0] = 3;
        break;
      }
    }
    System.out.println("LRU   sp    f");
    for (int i = 0; i < 4 ; i++){
      for (int j = 0; j < 3; j ++){
       System.out.print(TLB [i][j] + "    ");
      }
      System.out.println("");
    }
}

 int createMask(int a, int b){
    int r = 0;
    for (int i=a; i<=b; i++){
       r |= 1 << 31 - i;
    }
    return r;
  }

 void decrementLRU(int i){
  for (int j = 0 ; j < 4 ; j ++){
    if (TLB[j][0] > TLB [i][0]){
      TLB[j][0] --;
    }
  }
  TLB[i][0] = 3;
 }

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

 public static void main(String [] args) throws Exception{
  VirMem VM = new VirMem();
  VM.run();
 }

 void run() throws Exception{
  Scanner scfile = new Scanner(System.in);
  System.out.println("Please input path of file to initialize PM");  
  InputStream init = new FileInputStream(scfile.nextLine());
  InitializePM(init);
  
  //print BitMap-------------------------------------------------
  System.out.println("Initial BitMap Mapping: ");
   for(int a : BitMapFrames){
     System.out.println(String.format("%32s", Integer.toBinaryString(a)).replace(' ', '0'));
   }
   System.out.println("");
  //-------------------------------------------------------------
   
  //print initialized Physical Memory-----------------
   System.out.println("Physical Memory Allocation Mapping: ");
   for (int i = 0; i < PhyMem.length; i++){
     int a = PhyMem[i];
     if (a != 0){
     System.out.println(i + " = " + a + " , ");
     }
   }
   System.out.println("otherwise = 0");
   System.out.println("+++++++++++++++++++++++++++++++++++++++");
   
  //--------------------------------------------------
  System.out.println("Please input path of file with VAs to translate");
  InputStream VAs = new FileInputStream(scfile.nextLine());
  File f = new File("C:\\Users\\Nicholas\\Desktop\\A0112224B.txt");
  if (!f.exists()) {
        f.createNewFile();
      }
 
      fw = new FileWriter(f.getAbsoluteFile());
      translate(VAs);
      //print BitMap-------------------------------------------------
  System.out.println("Final BitMap Mapping: ");
   for(int a : BitMapFrames){
     System.out.println(String.format("%32s", Integer.toBinaryString(a)).replace(' ', '0'));
   }
   System.out.println("");
  //-------------------------------------------------------------
      fw.close();
      VAs.close();
      
 }}


