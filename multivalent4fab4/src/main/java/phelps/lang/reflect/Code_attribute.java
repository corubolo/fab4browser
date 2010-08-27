package phelps.lang.reflect;



/**
	@version $Revision$ $Date$
*/
public class Code_attribute extends Attribute_info {
  public int max_stack;	//u2
  public int max_locals; //u2
  public long code_length; //u4
  public int code_offset;	// into raw
  // byte[] code
  /*u2 exception_table_length;
	{	u2 start_pc;
		u2 end_pc;
		u2	handler_pc;
		u2	catch_type;
	}	exception_table[exception_table_length];
	u2 attributes_count;
  */
  Attribute_info[] attributes;

  /*
	l=local var index, L=two byte, c=constant pool index, C=two byte
	i=immediate
	if uppercase, two bytes
  */
  static final int
	NOP=0, ACONST_NULL=1,
	ICONST_M1=2, ICONST_0=3, ICONST_1=4, ICONST_2=5, ICONST_3=6, ICONST_4=7, ICONST_5=8,
	LCONST_0=9, LCONST_1=10,
	FCONST_0=11, FCONST_1=12, FCONST_2=13,
	DCONST_0=14, DCONST_1=15,
	BIPUSH=16, SIPUSH=17, LDC=18, LDC_W=19, LDC2_W=20,
	ILOAD=21, LLOAD=22, FLOAD=23, DLOAD=24, ALOAD=25,
	ILOAD_0=26, ILOAD_1=27, ILOAD_2=28, ILOAD_3=29,
	LLOAD_0=30, LLOAD_1=31, LLOAD_2=32, LLOAD_3=33,
	FLOAD_0=34, FLOAD_1=35, FLOAD_2=36, FLOAD_3=37,
	DLOAD_0=38, DLOAD_1=39, DLOAD_2=40, DLOAD_3=41,
	ALOAD_0=42, ALOAD_1=43, ALOAD_2=44, ALOAD_3=45,
	IALOAD=46, LALOAD=47, FALOAD=48, DALOAD=49, AALOAD=50, BALOAD=51, CALOAD=52, SALOAD=53,
	ISTORE=54, LSTORE=55, FSTORE=56, DSTORE=57, ASTORE=58,
	ISTORE_0=59, ISTORE_1=60, ISTORE_2=61, ISTORE_3=62,
	LSTORE_0=63, LSTORE_1=64, LSTORE_2=65, LSTORE_3=66,
	FSTORE_0=67, FSTORE_1=68, FSTORE_2=69, FSTORE_3=70,
	DSTORE_0=71, DSTORE_1=72, DSTORE_2=73, DSTORE_3=74,
	ASTORE_0=75, ASTORE_1=76, ASTORE_2=77, ASTORE_3=78,
	IASTORE=79, LASTORE=80, FASTORE=81, DASTORE=82, AASTORE=83, BASTORE=84, CASTORE=85, SASTORE=86,
	POP=87, POP2=88, DUP=89, DUP_X1=90, DUP_X2=91, DUP2=92, DUP2_X1=93, DUP2_X2=94, SWAP=95,
	IADD=96, LADD=97, FADD=98, DADD=99,
	ISUB=100, LSUB=101, FSUB=102, DSUB=103,
	IMUL=104, LMUL=105, FMUL=106, DMUL=107,
	IDIV=108, LDIV=109, FDIV=110, DDIV=111,
	IREM=112, LREM=113, FREM=114, DREM=115,
	INEG=116, LNEG=117, FNEG=118, DNEG=119,
	ISHL=120, LSHL=121, ISHR=122, LSHR=123,
	IUSHR=124, LUSHR=125, IAND=126, LAND=127, IOR=128, LOR=129, IXOR=130, LXOR=131, IINC=132,
	I2L=133, I2F=134, I2D=135, L2I=136, L2F=137, L2D=138, F2I=139, F2L=140, F2D=141, D2I=142, D2L=143, D2F=144, I2B=145, I2C=146, I2S=147,
	LCMP=148, FCMPL=149, FCMPG=150, DCMPL=151, DCMPG=152,
	IFEQ=153, IFNE=154, IFLT=155, IFGE=156, IFGT=157, IFLE=158, IF_ICMPEQ=159, IF_ICMPNE=160, IF_ICMPLT=161, IFICMPGE=162, IF_ICMPGT=163, IF_ICMPLE=164, IF_ACMPEQ=165, IF_ACMPNE=166,
	GOTO=167, JSR=168, RET=169, TABLESWITCH=170, LOOKUPSWITCH=171, IRETURN=172, LRETURN=173, FRETURN=174, DRETURN=175, ARETURN=176, RETURN=177,
	GETSTATIC=178, PUTSTATIC=179, GETFIELD=180, PUTFIELD=181,
	INVOKEVIRTUAL=182, INVOKESPECIAL=183, INVOKESTATIC=184, INVOKEINTERFACE=185,
	//UNUSED=186,?
	NEW=187, NEWARRAY=188, ANEWARRAY=189, ARRAYLENGTH=190,
	ATHROW=191, CHECKCAST=192, INSTANCEOF=193, MONITORENTER=194, MONITOREXIT=195, WIDE=196, MULTIANEWAARRAY=197,
	IFNULL=198, IFNONNULL=199, GOTO_W=200, JSR_W=201,
	BREAKPOINT=202
	;
  static String[] OPCODENAME, OPCODEARGS;
  static {
	String[] opinfo = {
		"nop",
		"aconst_null",
		"iconst_m1", "iconst_0", "iconst_1", "iconst_2", "iconst_3", "iconst_4", "iconst_5",
		"lconst_0", "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0", "dconst_1",
		"bipush/i", "sipush/I", "ldc/c", "ldc_w/C", "ldc2_w",
		"iload/l", "lload/l", "fload/l", "dload/l", "aload/l",
		"iload_0", "iload_1", "iload_2", "iload_3",
		"lload_0", "lload_1", "lload_2", "lload_3",
		"fload_0", "fload_1", "fload_2", "fload_3",
		"dload_0", "dload_1", "dload_2", "dload_3",
		"aload_0", "aload_1", "aload_2", "aload_3",
		"iaload", "laload", "faload", "daload", "aaload", "baload", "caload", "saload",
		"istore/l", "lstore/l", "fstore/l", "dstore/l", "astore/l",
		"istore_0", "istore_1", "istore_2", "istore_3",
		"lstore_0", "lstore_1", "lstore_2", "lstore_3",
		"fstore_0", "fstore_1", "fstore_2", "fstore_3",
		"dstore_0", "dstore_1", "dstore_2", "dstore_3",
		"astore_0", "astore_1", "astore_2", "astore_3",
		"iastore", "lastore", "fastore", "dastore", "aastore", "bastore", "castore", "sastore",
		"pop", "pop2", "dup", "dup_x1", "dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap",
		"iadd", "ladd", "fadd", "dadd",
		"isub", "lsub", "fsub", "dsub",
		"imul", "lmul", "fmul", "dmul",
		"idiv", "ldiv", "fdiv", "ddiv",
		"irem", "lrem", "frem", "drem",
		"ineg", "lneg", "fneg", "dneg",
		"ishl", "lshl", "ishr", "lshr",
		"iushr", "lushr", "iand", "land", "ior", "lor", "ixor", "lxor", "iinc/li",
		"i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l", "d2f", "i2b", "i2c", "i2s",
		"lcmp", "fcmpl", "fcmpg", "dcmpl", "dcmpg",
		"ifeq/I", "ifne/I", "iflt/I", "ifge/I", "ifgt/I", "ifle/I", "if_icmpeq/I", "if_icmpne/I", "if_icmplt/I", "if_icmpge/I", "if_icmpgt/I", "if_icmple/I", "if_acmpeq/I", "if_acmpne/I",
		"goto/I", "jsr/I", "ret/l", "tableswitch/w"/*variable size*/, "lookupswitch/w"/*variable size*/,
		"ireturn", "lreturn", "freturn", "dreturn", "areturn", "return",
		"getstatic/C", "putstatic/C", "getfield/C", "putfield/C",
		"invokevirtual/C", "invokespecial/C", "invokestatic/C", "invokeinterface/Cii",
		"xxxunusedxxx1",
		"new/C", "newarray/i", "anewarray/C", "arraylength",
		"athrow", "checkcast/C", "instanceof/I", "monitorenter", "monitorexit", "wide/iL", "multianewarray/Ci",
		"ifnull/I", "ifnonnull/I", "goto_w/W", "jsr_w/W",
		"breakpoint"
	};
	//Reserved opcodes: 0xfe "impdep1", 0xff "impdep2"

	OPCODENAME = new String[opinfo.length]; OPCODEARGS=new String[opinfo.length];
	for (int i=0,imax=opinfo.length; i<imax; i++) {
		String op=opinfo[i];
		int inx = op.indexOf('/');
		if (inx==-1) {
			OPCODENAME[i]=op; OPCODEARGS[i]=null;
		} else {
			OPCODENAME[i] = op.substring(0,inx);
			OPCODEARGS[i] = op.substring(inx+1);
			//String args = op.substring(inx+1);
			//opcodepcnts[i] = op.length()-inx;
			//int len=args.length(); for (int j=0,jmax=args.length(); j<jmax; j++) if (Character.isUpperCase(args.charAt(j))) len++;
			//opcodepcnts[i]=len;
		}
		OPCODENAME[i] = OPCODENAME[i].intern();
		//op2byte.put(OPCODENAME[i], new Integer(i));
	}

	assert OPCODENAME[BIPUSH]=="bipush"; assert OPCODENAME[ALOAD_0]=="aload_0"; assert OPCODENAME[IREM]=="irem";
	assert OPCODENAME[IFNULL]=="ifnull"; assert OPCODENAME[TABLESWITCH]=="tableswitch";
  }


  public Code_attribute(Cp_info[] cp, ClassFile cf, String name, int off) {
	super(cp, name, off);

	max_stack = cf.readu2();
	max_locals = cf.readu2();
	code_length = cf.readu4();
	code_offset = cf.getPosition();
	cf.setPosition(code_offset + (int)code_length);
	int exlen = cf.readu2(); cf.setPosition(cf.getPosition() + 8*exlen);		// skip exception table for now
	attributes = new Attribute_info[cf.readu2()];
	for (int i=0,imax=attributes.length; i<imax; i++) attributes[i] = cf.getAttributeInfo(cp);
  }


  public void getBasicBlocks(Cp_info[] cp, ClassFile cf) {
  }

  public void getInstructions(Cp_info[] cp, ClassFile cf) {
  }

  public void disasm(Cp_info[] cp, ClassFile cf) {
	LocalVariableTable_attribute localtable = (LocalVariableTable_attribute)cf.getAttribute(attributes, "LocalVariableTable");
	// could dump locals at top of code
	//localtable=null;
//System.out.println("locals = "+localtable);
	//System.out.println(toString());
	cf.setPosition(code_offset);
	/*long u8;*/ int u4,uu1; short u2; //byte u1;
	for (int imax=code_offset+(int)code_length; cf.getPosition()<imax; ) {
		int op = cf.readuu1();
		String opname = OPCODENAME[op];
		System.out.print("\t"+Integer.toHexString(cf.getPosition()-code_offset-1)+"  "+opname+"/"+Integer.toHexString(op));
		String args = OPCODEARGS[op];
		int inx;
		if (op==0xaa || op==0xab) {  // variable length
			System.exit(0);
		} else if ((inx=opname.indexOf("const_"))!=-1) {
			try {
				//System.out.print(" #"+opname.substring(inx+5)/*+", locals="+localtable*/);
				System.out.print(" l/"+localtable.getLocal(Integer.parseInt(opname.substring(inx+5))));
			} catch (NumberFormatException shouldnthappen) {}
			//System.out.print(" #"+opname.substring(inx+6));
		} else if (op!=1 && ((inx=opname.indexOf("load_"))!=-1 || (inx=opname.indexOf("tore_"))!=-1)) {
			try {
				//System.out.print(" #"+opname.substring(inx+5)/*+", locals="+localtable*/);
				if (localtable!=null) System.out.print(" l/"+localtable.getLocal(Integer.parseInt(opname.substring(inx+5))));
				else System.out.print(" l/#"+opname.substring(inx+5));
			} catch (NumberFormatException shouldnthappen) {}
		} else if (OPCODEARGS[op]!=null) {
			for (int j=0,jmax=args.length(); j<jmax; j++) {
				String val=""; String type=" ";
//System.out.println("args="+args);
				switch (args.charAt(j)) {
				case 'i': type="#"; uu1=cf.readuu1(); val="0x"+Integer.toHexString(uu1); break;
				case 'I': type="#"; u2=cf.readu2(); val="0x"+Integer.toHexString(u2); break;
				case 'l': type="l/"; uu1=cf.readuu1(); val=(localtable!=null? localtable.getLocal(uu1).name: "0x"+Integer.toHexString(uu1)); break;
				case 'L': type="l/"; u2=cf.readu2(); val=(localtable!=null? localtable.getLocal(u2).name: "0x"+Integer.toHexString(u2)); break;
				case 'c': type="c/"; uu1=cf.readuu1(); val=cp[uu1].getString(cp, cf); break;
				case 'C': type="c/"; u2=cf.readu2(); val=cp[u2].getString(cp, cf); break;
				case 'W': type="#"; u4=cf.readu4(); val=Integer.toHexString(u4); break;
				}
				System.out.print(" "+type+val);
			}
		}
		System.out.println();
	}
  }

  public String toString() {
	StringBuffer sb = new StringBuffer(100);
	sb.append(attribute_name)/*.append(", len="+attribute_length)*/.append(", code len=").append(code_length);
	//sb.append(':');
	//for (int i=0,imax=Math.min(20,(int)code_length); i<imax; i++) sb.append(' ').append(Integer.toHexString(((int)cf.raw[code_offset+i])&0xff));
	return sb.substring(0);
  }
}
