/*
 * Copyright (C)2011 D. R. Commander.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the libjpeg-turbo Project nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS",
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This program tests the various code paths in the TurboJPEG JNI Wrapper
 */

import java.io.*;
import java.util.*;
import org.libjpegturbo.turbojpeg.*;

public class TJUnitTest {

	private static final String classname=new TJUnitTest().getClass().getName();

	private static void usage() {
		System.out.println("\nUSAGE: java "+classname+" [options]\n");
		System.out.println("Options:\n");
		System.out.println("-yuv = test YUV encoding/decoding support\n");
		System.exit(1);
	}

	private final static String _subnamel[]=
		{"4:4:4", "4:2:2", "4:2:0", "GRAY"};
	private final static String _subnames[]=
		{"444", "422", "420", "GRAY"};
	private final static int _hsf[]={1, 2, 2, 1};
	private final static int _vsf[]={1, 1, 2, 1};

	private final static String _pixformatstr[]=
		{"RGB", "BGR", "RGBX", "BGRX", "XBGR", "XRGB", "Grayscale"};
	private final static int _roffset[]=
		{0, 2, 0, 2, 3, 1, 0};
	private final static int _goffset[]=
		{1, 1, 1, 1, 2, 2, 0};
	private final static int _boffset[]=
		{2, 0, 2, 0, 1, 3, 0};

	private final static int _3byteFormats[]=
		{TJ.PF_RGB, TJ.PF_BGR};
	private final static int _4byteFormats[]=
		{TJ.PF_RGBX, TJ.PF_BGRX, TJ.PF_XBGR, TJ.PF_XRGB};
	private final static int _onlyGray[]=
		{TJ.PF_GRAY};
	private final static int _onlyRGB[]=
		{TJ.PF_RGB};

	private final static int YUVENCODE=1, YUVDECODE=2;
	private static int yuv=0;

	private static int exitstatus=0;

	private static double gettime()
	{
		return (double)System.nanoTime()/1.0e9;
	}

	private final static byte pixels[][]=
	{
		{0, (byte)255, 0},
		{(byte)255, 0, (byte)255},
		{(byte)255, (byte)255, 0},
		{0, 0, (byte)255},
		{0, (byte)255, (byte)255},
		{(byte)255, 0, 0},
		{(byte)255, (byte)255, (byte)255},
		{0, 0, 0},
		{(byte)255, 0, 0}
	};

	private static void initbuf(byte [] buf, int w, int h, int pf, int flags)
	{
		int roffset=_roffset[pf], goffset=_goffset[pf], boffset=_boffset[pf];
		int ps=TJ.pixelSize[pf];
		int i, _i, j;

		Arrays.fill(buf, (byte)0);
		if(pf==TJ.PF_GRAY)
		{
			for(_i=0; _i<16; _i++)
			{
				if((flags&TJ.BOTTOMUP)!=0) i=h-_i-1;  else i=_i;
				for(j=0; j<w; j++)
				{
					if(((_i/8)+(j/8))%2==0) buf[w*i+j]=(byte)255;
					else buf[w*i+j]=76;
				}
			}
			for(_i=16; _i<h; _i++)
			{
				if((flags&TJ.BOTTOMUP)!=0) i=h-_i-1;  else i=_i;
				for(j=0; j<w; j++)
				{
					if(((_i/8)+(j/8))%2==0) buf[w*i+j]=0;
					else buf[w*i+j]=(byte)226;
				}
			}
			return;
		}
		for(_i=0; _i<16; _i++)
		{
			if((flags&TJ.BOTTOMUP)!=0) i=h-_i-1;  else i=_i;
			for(j=0; j<w; j++)
			{
				buf[(w*i+j)*ps+roffset]=(byte)255;
				if(((_i/8)+(j/8))%2==0)
				{
					buf[(w*i+j)*ps+goffset]=(byte)255;
					buf[(w*i+j)*ps+boffset]=(byte)255;
				}
			}
		}
		for(_i=16; _i<h; _i++)
		{
			if((flags&TJ.BOTTOMUP)!=0) i=h-_i-1;  else i=_i;
			for(j=0; j<w; j++)
			{
				if(((_i/8)+(j/8))%2!=0)
				{
					buf[(w*i+j)*ps+roffset]=(byte)255;
					buf[(w*i+j)*ps+goffset]=(byte)255;
				}
			}
		}
	}

	private static void dumpbuf(byte [] buf, int w, int h, int pf,
		int scalefactor, int flags)
	{
		int ps=TJ.pixelSize[pf];
		int roffset=_roffset[pf], goffset=_goffset[pf], boffset=_boffset[pf];
		int i, j;

		System.out.print("\n");
		for(i=0; i<h; i++)
		{
			for(j=0; j<w; j++)
			{
				int r=buf[(w*i+j)*ps+roffset];
				int g=buf[(w*i+j)*ps+goffset];
				int b=buf[(w*i+j)*ps+boffset];
				if(r<0) r+=256;  if(g<0) g+=256;  if(b<0) b+=256;
				System.out.format("%3d/%3d/%3d ", r, g, b);
			}
			System.out.print("\n");
		}
	}

	private static void checkval(int i, int j, byte v, String vname, int cvi)
		throws Exception
	{
		int vi=(v<0)? v+256:v;
		if(vi<cvi-1 || vi>cvi+1)
		{
			throw new Exception("\nComp. "+vname+" at "+i+","+j+" should be "+cvi
				+", not "+v+"\n");
		}
	}

	private static void checkval0(int i, int j, byte v, String vname)
		throws Exception
	{
		int vi=(v<0)? v+256:v;
		if(vi>1)
		{
			throw new Exception("\nComp. "+vname+" at "+i+","+j+" should be 0, not "
				+v+"\n");
		}
	}

	private static void checkval255(int i, int j, byte v, String vname)
		throws Exception
	{
		int vi=(v<0)? v+256:v;
		if(vi<254 && !(vi==217 && i==0 && j==21))
		{
			throw new Exception("\nComp. "+vname+" at "+i+","+j+" should be 255, not "
				+v+"\n");
		}
	}

	private static int checkbuf(byte [] buf, int w, int h, int pf, int subsamp,
		int scalefactor, int flags) throws Exception
	{
		int roffset=_roffset[pf], goffset=_goffset[pf], boffset=_boffset[pf];
		int ps=TJ.pixelSize[pf];
		int i, _i, j, retval=1;
		int halfway=16/scalefactor, blocksize=8/scalefactor;

		try
		{
			if(subsamp==TJ.SAMP_GRAY)
			{
				for(_i=0; _i<halfway; _i++)
				{
					if((flags&TJ.BOTTOMUP)!=0) i=h-_i-1;  else i=_i;
					for(j=0; j<w; j++)
					{
						byte r=buf[(w*i+j)*ps+roffset],
							g=buf[(w*i+j)*ps+goffset],
							b=buf[(w*i+j)*ps+boffset];
						if(((_i/blocksize)+(j/blocksize))%2==0)
						{
							checkval255(_i, j, r, "R");
							checkval255(_i, j, g, "G");
							checkval255(_i, j, b, "B");
						}
						else
						{
							checkval(_i, j, r, "R", 76);
							checkval(_i, j, g, "G", 76);
							checkval(_i, j, b, "B", 76);
						}
					}
				}
				for(_i=halfway; _i<h; _i++)
				{
					if((flags&TJ.BOTTOMUP)!=0) i=h-_i-1;  else i=_i;
					for(j=0; j<w; j++)
					{
						byte r=buf[(w*i+j)*ps+roffset],
							g=buf[(w*i+j)*ps+goffset],
							b=buf[(w*i+j)*ps+boffset];
						if(((_i/blocksize)+(j/blocksize))%2==0)
						{
							checkval0(_i, j, r, "R");
							checkval0(_i, j, g, "G");
							checkval0(_i, j, b, "B");
						}
						else
						{
							checkval(_i, j, r, "R", 226);
							checkval(_i, j, g, "G", 226);
							checkval(_i, j, b, "B", 226);
						}
					}
				}
			}
			else
			{
				for(_i=0; _i<halfway; _i++)
				{
					if((flags&TJ.BOTTOMUP)!=0) i=h-_i-1;  else i=_i;
					for(j=0; j<w; j++)
					{
						checkval255(_i, j, buf[(w*i+j)*ps+roffset], "R");
						if(((_i/blocksize)+(j/blocksize))%2==0)
						{
							checkval255(_i, j, buf[(w*i+j)*ps+goffset], "G");
							checkval255(_i, j, buf[(w*i+j)*ps+boffset], "B");
						}
						else
						{
							checkval0(_i, j, buf[(w*i+j)*ps+goffset], "G");
							checkval0(_i, j, buf[(w*i+j)*ps+boffset], "B");
						}
					}
				}
				for(_i=halfway; _i<h; _i++)
				{
					if((flags&TJ.BOTTOMUP)!=0) i=h-_i-1;  else i=_i;
					for(j=0; j<w; j++)
					{
						checkval0(_i, j, buf[(w*i+j)*ps+boffset], "B");
						if(((_i/blocksize)+(j/blocksize))%2==0)
						{
							checkval0(_i, j, buf[(w*i+j)*ps+roffset], "R");
							checkval0(_i, j, buf[(w*i+j)*ps+goffset], "G");
						}
						else
						{
							checkval255(_i, j, buf[(w*i+j)*ps+roffset], "R");
							checkval255(_i, j, buf[(w*i+j)*ps+goffset], "G");
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println(e);
			retval=0;
		}
		return retval;
	}

	private static int PAD(int v, int p)
	{
		return ((v+(p)-1)&(~((p)-1)));
	}

	private static int checkbufyuv(byte [] buf, int size, int w, int h,
		int subsamp)
	{
		int i, j;
		int hsf=_hsf[subsamp], vsf=_vsf[subsamp];
		int pw=PAD(w, hsf), ph=PAD(h, vsf);
		int cw=pw/hsf, ch=ph/vsf;
		int ypitch=PAD(pw, 4), uvpitch=PAD(cw, 4);
		int retval=1;
		int correctsize=ypitch*ph + (subsamp==TJ.SAMP_GRAY? 0:uvpitch*ch*2);

		try
		{
			if(size!=correctsize)
				throw new Exception("\nIncorrect size "+size+".  Should be "
					+correctsize);

			for(i=0; i<16; i++)
			{
				for(j=0; j<pw; j++)
				{
					byte y=buf[ypitch*i+j];
					if(((i/8)+(j/8))%2==0) checkval255(i, j, y, "Y");
					else checkval(i, j, y, "Y", 76);
				}
			}
			for(i=16; i<ph; i++)
			{
				for(j=0; j<pw; j++)
				{
					byte y=buf[ypitch*i+j];
					if(((i/8)+(j/8))%2==0) checkval0(i, j, y, "Y");
					else checkval(i, j, y, "Y", 226);
				}
			}
			if(subsamp!=TJ.SAMP_GRAY)
			{
				for(i=0; i<16/vsf; i++)
				{
					for(j=0; j<cw; j++)
					{
						byte u=buf[ypitch*ph + (uvpitch*i+j)],
							v=buf[ypitch*ph + uvpitch*ch + (uvpitch*i+j)];
						if(((i*vsf/8)+(j*hsf/8))%2==0)
						{
							checkval(i, j, u, "U", 128);  checkval(i, j, v, "V", 128);
						}
						else
						{
							checkval(i, j, u, "U", 85);  checkval255(i, j, v, "V");
						}
					}
				}
				for(i=16/vsf; i<ch; i++)
				{
					for(j=0; j<cw; j++)
					{
						byte u=buf[ypitch*ph + (uvpitch*i+j)],
							v=buf[ypitch*ph + uvpitch*ch + (uvpitch*i+j)];
						if(((i*vsf/8)+(j*hsf/8))%2==0)
						{
							checkval(i, j, u, "U", 128);  checkval(i, j, v, "V", 128);
						}
						else
						{
							checkval0(i, j, u, "U");  checkval(i, j, v, "V", 149);
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println(e);
			retval=0;
		}

		if(retval==0)
		{
			for(i=0; i<ph; i++)
			{
				for(j=0; j<pw; j++)
				{
					int y=buf[ypitch*i+j];
					if(y<0) y+=256;
					System.out.format("%3d ", y);
				}
				System.out.print("\n");
			}
			System.out.print("\n");
			for(i=0; i<ch; i++)
			{
				for(j=0; j<cw; j++)
				{
					int u=buf[ypitch*ph + (uvpitch*i+j)];
					if(u<0) u+=256;
					System.out.format("%3d ", u);
				}
				System.out.print("\n");
			}
			System.out.print("\n");
			for(i=0; i<ch; i++)
			{
				for(j=0; j<cw; j++)
				{
					int v=buf[ypitch*ph + uvpitch*ch + (uvpitch*i+j)];
					if(v<0) v+=256;
					System.out.format("%3d ", v);
				}
				System.out.print("\n");
			}
			System.out.print("\n");
		}

		return retval;
	}

	private static void writejpeg(byte [] jpegbuf, int jpgbufsize,
		String filename) throws Exception
	{
		File file=new File(filename);
		FileOutputStream fos=new FileOutputStream(file);
		fos.write(jpegbuf, 0, jpgbufsize);
		fos.close();
	}

	private static int gentestjpeg(TJCompressor tjc, byte [] jpegbuf, int w,
		int h, int pf, String basefilename, int subsamp, int qual,
		int flags) throws Exception
	{
		String tempstr;  byte [] bmpbuf;
		String pixformat;  double t;
		int size=0;

		if(yuv==YUVENCODE) flags|=TJ.YUV;

		pixformat=_pixformatstr[pf];

		System.out.print(pixformat+" ");
		if((flags&TJ.BOTTOMUP)!=0) System.out.print("Bottom-Up");
		else System.out.print("Top-Down ");
		System.out.print(" -> "+_subnamel[subsamp]+" ");
		if(yuv==YUVENCODE) System.out.print("YUV ... ");
		else System.out.print("Q"+qual+" ... ");

		bmpbuf=new byte[w*h*TJ.pixelSize[pf]+1];
		initbuf(bmpbuf, w, h, pf, flags);
		Arrays.fill(jpegbuf, (byte)0);

		t=gettime();
		tjc.setBitmapBuffer(bmpbuf, w, 0, h, pf);
		size=tjc.compress(jpegbuf, subsamp, qual, flags);
		t=gettime()-t;

		if(yuv==YUVENCODE)
			tempstr=basefilename+"_enc_"+pixformat+"_"
				+(((flags&TJ.BOTTOMUP)!=0)? "BU":"TD")+"_"+_subnames[subsamp]+".yuv";
		else
			tempstr=basefilename+"_enc_"+pixformat+"_"
				+(((flags&TJ.BOTTOMUP)!=0)? "BU":"TD")+"_"+_subnames[subsamp]
				+"_Q"+qual+".jpg";
		writejpeg(jpegbuf, size, tempstr);
		if(yuv==YUVENCODE)
		{
			if(checkbufyuv(jpegbuf, size, w, h, subsamp)==1)
				System.out.print("Passed.");
			else {System.out.print("FAILED!");  exitstatus=-1;}
		}
		else System.out.print("Done.");
		System.out.format("  %.6f ms\n", t*1000.);
		System.out.println("  Result in "+tempstr);

		return size;
	}

	private static void _gentestbmp(TJDecompressor tjd, byte [] jpegbuf,
		int jpegsize, int w, int h, int pf, String basefilename, int subsamp,
		int flags, int scalefactor) throws Exception
	{
		String pixformat;  int _hdrw=0, _hdrh=0, _hdrsubsamp=-1;  double t;
		int scaledw=(w+scalefactor-1)/scalefactor;
		int scaledh=(h+scalefactor-1)/scalefactor;
		int temp1, temp2;

		if(yuv==YUVDECODE) flags|=TJ.YUV;
		else if(yuv==YUVENCODE) return;

		pixformat=_pixformatstr[pf];
		System.out.print("JPEG -> ");
		if(yuv==YUVDECODE)
			System.out.print("YUV "+_subnames[subsamp]+" ... ");
		else
		{
			System.out.print(pixformat+" ");
			if((flags&TJ.BOTTOMUP)!=0) System.out.print("Bottom-Up ");
			else System.out.print("Top-Down  ");
			if(scalefactor!=1) System.out.print("1/"+scalefactor+" ... ");
			else System.out.print("... ");
		}

		t=gettime();
		tjd.setJPEGBuffer(jpegbuf, jpegsize);
		if(tjd.getWidth()!=w || tjd.getHeight()!=h || tjd.getSubsamp()!=subsamp)
			throw new Exception("Incorrect JPEG header");

		temp1=scaledw;  temp2=scaledh;
		temp1=tjd.getScaledWidth(temp1, temp2);
		temp2=tjd.getScaledHeight(temp1, temp2);
		if(temp1!=scaledw || temp2!=scaledh)
			throw new Exception("Scaled size mismatch");

		byte [] bmpbuf=tjd.decompress(scaledw, 0, scaledh, pf, flags);
		t=gettime()-t;

		if(yuv==YUVDECODE)
		{
			if(checkbufyuv(bmpbuf, bmpbuf.length, w, h, subsamp)==1)
				System.out.print("Passed.");
			else {System.out.print("FAILED!");  exitstatus=-1;}
		}
		else
		{
			if(checkbuf(bmpbuf, scaledw, scaledh, pf, subsamp, scalefactor, flags)
				==1)
				System.out.print("Passed.");
			else
			{
				System.out.print("FAILED!");  exitstatus=-1;
				dumpbuf(bmpbuf, scaledw, scaledh, pf, scalefactor, flags);
			}
		}
		System.out.format("  %.6f ms\n", t*1000.);
	}

	private static void gentestbmp(TJDecompressor tjd, byte [] jpegbuf,
		int jpegsize, int w, int h, int pf, String basefilename, int subsamp,
		int flags) throws Exception
	{
		int i;
		if((subsamp==TJ.SAMP_444 || subsamp==TJ.SAMP_GRAY) && yuv==0)
		{
			for(i=1; i<=8; i*=2)
				_gentestbmp(tjd, jpegbuf, jpegsize, w, h, pf, basefilename, subsamp,
					flags, i);
		}
		else
			_gentestbmp(tjd, jpegbuf, jpegsize, w, h, pf, basefilename, subsamp,
				flags, 1);
		System.out.print("\n");
	}

	private static void dotest(int w, int h, int [] formats, int subsamp,
		String basefilename) throws Exception
	{
		TJCompressor tjc=null;  TJDecompressor tjd=null;
		int size;  int pfstart, pfend;

		byte [] jpegbuf=new byte[TJ.bufSize(w, h)];

		try
		{
			tjc=new TJCompressor();
			tjd=new TJDecompressor();  

			for(int pf : formats)
			{
				for(int i=0; i<2; i++)
				{
					int flags=0;
					if(i==1)
					{
						if(yuv==YUVDECODE)
						{
							tjc.close();  tjd.close();  return;
						}
						else flags|=TJ.BOTTOMUP;
					}
					size=gentestjpeg(tjc, jpegbuf, w, h, pf, basefilename, subsamp, 100,
						flags);
					gentestbmp(tjd, jpegbuf, size, w, h, pf, basefilename, subsamp,
						flags);
				}
			}
		}
		catch(Exception e)
		{
			if(tjc!=null) tjc.close();
			if(tjd!=null) tjd.close();
			throw e;
		}
		if(tjc!=null) tjc.close();
		if(tjd!=null) tjd.close();
	}

	private final static int MAXLENGTH=2048;

	private static void dotest1() throws Exception
	{
		int i, j, i2;  byte [] bmpbuf, jpgbuf;
		TJCompressor tjc=null;  int size;

		try
		{
			tjc=new TJCompressor();
			System.out.println("Buffer size regression test");
			for(j=1; j<48; j++)
			{
				for(i=1; i<(j==1?MAXLENGTH:48); i++)
				{
					if(i%100==0) System.out.format("%04d x %04d\b\b\b\b\b\b\b\b\b\b\b",
						i, j);
					bmpbuf=new byte[i*j*4];
					jpgbuf=new byte[TJ.bufSize(i, j)];
					Arrays.fill(bmpbuf, (byte)0);
					for(i2=0; i2<i*j; i2++)
					{
						bmpbuf[i2*4]=pixels[i2%9][2];
						bmpbuf[i2*4+1]=pixels[i2%9][1];
						bmpbuf[i2*4+2]=pixels[i2%9][0];
					}
					tjc.setBitmapBuffer(bmpbuf, i, 0, j, TJ.PF_BGRX);
					size=tjc.compress(jpgbuf, TJ.SAMP_444, 100, 0);

					bmpbuf=new byte[j*i*4];
					jpgbuf=new byte[TJ.bufSize(j, i)];
					for(i2=0; i2<j*i; i2++)
					{
						if(i2%2==0) bmpbuf[i2*4]=bmpbuf[i2*4+1]=bmpbuf[i2*4+2]=(byte)0xFF;
						else bmpbuf[i2*4]=bmpbuf[i2*4+1]=bmpbuf[i2*4+2]=0;
					}
					tjc.setBitmapBuffer(bmpbuf, j, 0, i, TJ.PF_BGRX);
					size=tjc.compress(jpgbuf, TJ.SAMP_444, 100, 0);
				}
			}
			System.out.println("Done.      ");
		}
		catch(Exception e)
		{
			if(tjc!=null) tjc.close();
			throw e;
		}
		if(tjc!=null) tjc.close();
	}

	public static void main(String argv[])
	{
		try
		{
			boolean doyuv=false;
			for(int i=0; i<argv.length; i++)
			{
				if(argv[i].equalsIgnoreCase("-yuv")) doyuv=true;
				if(argv[i].substring(0, 1).equalsIgnoreCase("-h")
					|| argv[i].equalsIgnoreCase("-?"))
					usage();
			}
			if(doyuv) yuv=YUVENCODE;
			dotest(35, 39, _3byteFormats, TJ.SAMP_444, "test");
			dotest(39, 41, _4byteFormats, TJ.SAMP_444, "test");
			if(doyuv)
			{
				dotest(41, 35, _3byteFormats, TJ.SAMP_422, "test");
				dotest(35, 39, _4byteFormats, TJ.SAMP_422, "test");
				dotest(39, 41, _3byteFormats, TJ.SAMP_420, "test");
				dotest(41, 35, _4byteFormats, TJ.SAMP_420, "test");
			}
			dotest(35, 39, _onlyGray, TJ.SAMP_GRAY, "test");
			dotest(39, 41, _3byteFormats, TJ.SAMP_GRAY, "test");
			dotest(41, 35, _4byteFormats, TJ.SAMP_GRAY, "test");
			if(!doyuv) dotest1();
			if(doyuv)
			{
				yuv=YUVDECODE;
				dotest(48, 48, _onlyRGB, TJ.SAMP_444, "test");
				dotest(35, 39, _onlyRGB, TJ.SAMP_444, "test");
				dotest(48, 48, _onlyRGB, TJ.SAMP_422, "test");
				dotest(39, 41, _onlyRGB, TJ.SAMP_422, "test");
				dotest(48, 48, _onlyRGB, TJ.SAMP_420, "test");
				dotest(41, 35, _onlyRGB, TJ.SAMP_420, "test");
				dotest(48, 48, _onlyGray, TJ.SAMP_GRAY, "test");
				dotest(35, 39, _onlyGray, TJ.SAMP_GRAY, "test");
			}
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		System.exit(exitstatus);
	}
}