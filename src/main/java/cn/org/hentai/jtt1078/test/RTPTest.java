package cn.org.hentai.jtt1078.test;

import cn.org.hentai.jtt1078.flv.FlvEncoder;
import cn.org.hentai.jtt1078.server.Jtt1078Decoder;
import cn.org.hentai.jtt1078.util.ByteHolder;
import cn.org.hentai.jtt1078.util.ByteUtils;
import cn.org.hentai.jtt1078.util.Packet;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by matrixy on 2019/12/16.
 */
public class RTPTest
{
    public static void main(String[] args) throws Exception
    {
        InputStream fis = new FileInputStream("d:\\streamax-20191209.bin");
        int len = -1;
        byte[] block = new byte[1024];
        Jtt1078Decoder decoder = new Jtt1078Decoder();

        int bufferOffset = 0;
        ByteHolder buffer = new ByteHolder(1096 * 100);
        String profile = null;

        FileOutputStream fos = new FileOutputStream("d:\\flvanalyser.flv");
        FlvEncoder flvEncoder = new FlvEncoder(true, false);

        int timestamp = 0;
        boolean xxoo = false;

        while ((len = fis.read(block)) > -1)
        {
            decoder.write(block, 0, len);
            while (true)
            {
                Packet p = decoder.decode();
                if (p == null) break;
                int lengthOffset = 28;
                int dataType = (p.seek(15).nextByte() >> 4) & 0x0f;
                // 透传数据类型：0100，没有后面的时间以及Last I Frame Interval和Last Frame Interval字段
                if (dataType == 0x04) lengthOffset = 28 - 8 - 2 - 2;
                else if (dataType == 0x03) lengthOffset = 28 - 4;

                int pkType = p.seek(15).nextByte() & 0x0f;

                if (dataType == 0x03 || dataType == 0x04) continue;
                if (dataType == 0x00 || dataType == 0x01 || dataType == 0x02)
                {
                    buffer.write(p.seek(lengthOffset + 2).nextBytes());

                    // 再读出所有缓存的nalu单元
                    for (int i = 0; i < buffer.size(); i++)
                    {
                        int a = buffer.get(i + 0) & 0xff;
                        int b = buffer.get(i + 1) & 0xff;
                        int c = buffer.get(i + 2) & 0xff;
                        int d = buffer.get(i + 3) & 0xff;
                        if (a == 0x00 && b == 0x00 && c == 0x00 && d == 0x01)
                        {
                            if (i == 0) continue;
                            byte[] nalu = new byte[i];
                            buffer.sliceInto(nalu, i);
                            i = 0;

                            if (nalu.length < 4) continue;

                            byte[] data = flvEncoder.write(nalu, timestamp += 67);

                            if (xxoo == false && flvEncoder.videoReady())
                            {
                                fos.write(flvEncoder.getHeader().getBytes());
                                fos.write(flvEncoder.getVideoHeader().getBytes());
                                xxoo = true;
                            }

                            if (data != null) fos.write(data);
                        }
                    }
                }
            }
        }

        fos.close();

        // flvEncoder.close();
    }


    static int U(int iBitCount, byte[] bData, int iStartBit)
    {
        int iRet = 0;
        for (int i = 0; i < iBitCount; i++)
        {
            iRet = iRet << 1;
            if ((0x80 >> (iStartBit % 8)) == (bData[iStartBit / 8] & (0x80 >> (iStartBit % 8))))
            {
                iRet += 1;
            }
            iStartBit++;
        }
        return iRet;
    }

    static int Ue(byte[] bData, int iStartBit)
    {
        int nZeroNum = 0;
        while (iStartBit < bData.length * 8)
        {
            if ((0x80 >> (iStartBit % 8)) == (bData[iStartBit / 8] & (0x80 >> (iStartBit % 8)))) //&:按位与，%取余
            {
                break;
            }
            nZeroNum++;
            iStartBit++;
        }
        nZeroNum = nZeroNum + 1;

        int dwRet = 0;
        for (int i = 0; i < nZeroNum; i++)
        {
            dwRet <<= 1;
            if ((0x80 >> (iStartBit % 8)) == (bData[iStartBit / 8] & (0x80 >> (iStartBit % 8))))
            {
                dwRet += 1;
            }
            iStartBit++;
        }
        return dwRet-1;
    }


    static int Se(byte[] bData, int iStartBit)
    {
        int nZeroNum = 0;
        int f = 0;
        while (iStartBit < bData.length * 8)
        {
            if ((0x80 >> (iStartBit % 8)) == (bData[iStartBit / 8] & (0x80 >> (iStartBit % 8)))) //&:按位与，%取余
            {
                break;
            }
            nZeroNum++;
            iStartBit++;
            f++;
        }
        //计算结果
        int dwRet = 0;
        for (int i = 0; i < nZeroNum; i++)
        {
            dwRet <<= 1;
            if ((0x80 >> (iStartBit % 8)) == (bData[iStartBit / 8] & (0x80 >> (iStartBit % 8))))
            {
                dwRet += 1;
            }
            iStartBit++;
            f++;
        }
        if ((0x80 >> (iStartBit % 8)) == (bData[iStartBit / 8] & (0x80 >> (iStartBit % 8))))
        {
            dwRet = 0-dwRet;
        }
        iStartBit++;
        f++;
        System.out.println("Fuck: " + f);
        return dwRet;
    }

    private static void parseSPS(byte[] bData, int StartBit)
    {
        int profile_idc = U(8, bData, StartBit += 0);
        int constraint_set0_flag = U(1, bData, StartBit += 8);
        int constraint_set1_flag = U(1, bData, StartBit += 1);
        int constraint_set2_flag = U(1, bData, StartBit += 1);
        int constraint_set3_flag = U(1, bData, StartBit += 1);
        int reserved_zero_4bits = U(4, bData, StartBit += 1);
        int level_idc = U(8, bData, StartBit += 4);
        int seq_parameter_set_id = Ue(bData, StartBit += 8);
        int chroma_format_idc = 0;
        if (profile_idc == 100 || profile_idc == 110 ||
                profile_idc == 122 || profile_idc == 144)
        {
            chroma_format_idc = Ue(bData, StartBit += 7);
            if (chroma_format_idc == 3)
            {
                int residual_colour_transform_flag = U(1, bData, StartBit += 7);
            }
            int bit_depth_luma_minus8 = Ue(bData, StartBit += 1);
            int bit_depth_chroma_minus8 = Ue(bData, StartBit += 7);
            int qpprime_y_zero_transform_bypass_flag = U(1, bData, StartBit += 7);
            int seq_scaling_matrix_present_flag = U(1, bData, StartBit += 1);

            int[] seq_scaling_list_present_flag = new int[8];
            if (1 == seq_scaling_matrix_present_flag)
            {
                for (int i = 0; i < 8; i++)
                {
                    seq_scaling_list_present_flag[i] = U(1, bData, StartBit += 1);
                }
            }
        }
        int log2_max_frame_num_minus4 = Ue(bData, StartBit += 1);
        int pic_order_cnt_type = Ue(bData, StartBit += 7);
        if (pic_order_cnt_type == 0)
        {
            int log2_max_pic_order_cnt_lsb_minus4 = Ue(bData, StartBit += 7);
        }
        else if (pic_order_cnt_type == 1)
        {
            int delta_pic_order_always_zero_flag = U(1, bData, StartBit += 7);
            int offset_for_non_ref_pic = Se(bData, StartBit += 1);
            int offset_for_top_to_bottom_field = Se(bData, StartBit += 8);
            int num_ref_frames_in_pic_order_cnt_cycle = Ue(bData, StartBit += 8);

            int[] offset_for_ref_frame = new int[num_ref_frames_in_pic_order_cnt_cycle];
            for (int i = 0; i < num_ref_frames_in_pic_order_cnt_cycle; i++)
                offset_for_ref_frame[i] = Se(bData, StartBit += 7);
        }
        int num_ref_frames = Ue(bData, StartBit += 8);
        int gaps_in_frame_num_value_allowed_flag = U(1, bData, StartBit += 7);
        int pic_width_in_mbs_minus1 = Ue(bData, StartBit += 1);
        int pic_height_in_map_units_minus1 = Ue(bData, StartBit += 7);


        int frame_mbs_only_flag = U(1, bData, StartBit += 7);
        if (0 == frame_mbs_only_flag)
        {
            int mb_adaptive_frame_field_flag = U(1, bData, StartBit += 1);
        }
        int direct_8x8_inference_flag = U(1, bData, StartBit += 1);
        int frame_cropping_flag = U(1, bData, StartBit += 1);

        int frame_crop_left_offset = 0;
        int frame_crop_right_offset = 0;
        int frame_crop_top_offset = 0;
        int frame_crop_bottom_offset = 0;

        if (1 == frame_cropping_flag)
        {
            frame_crop_left_offset = Ue(bData, StartBit += 1);
            frame_crop_right_offset = Ue(bData, StartBit += 7);
            frame_crop_top_offset = Ue(bData, StartBit += 7);
            frame_crop_bottom_offset = Ue(bData, StartBit += 7);
        }
        int vui_parameter_present_flag = U(1, bData, StartBit += 7);
        if (1 == vui_parameter_present_flag)
        {
            int aspect_ratio_info_present_flag = U(1, bData, StartBit += 1);
            if (1 == aspect_ratio_info_present_flag)
            {
                int aspect_ratio_idc = U(8, bData, StartBit += 1);
                if (aspect_ratio_idc == 255)
                {
                    int sar_width = U(16, bData, StartBit += 8);
                    int sar_height = U(16, bData, StartBit += 16);
                }
            }
            int overscan_info_present_flag = U(1, bData, StartBit += 16);
            if (1 == overscan_info_present_flag)
            {
                int overscan_appropriate_flagu = U(1, bData, StartBit += 1);
            }
            int video_signal_type_present_flag = U(1, bData, StartBit += 1);
            if (1 == video_signal_type_present_flag)
            {
                int video_format = U(3, bData, StartBit += 1);
                int video_full_range_flag = U(1, bData, StartBit += 3);
                int colour_description_present_flag = U(1, bData, StartBit += 1);
                if (1 == colour_description_present_flag)
                {
                    int colour_primaries = U(8, bData, StartBit += 1);
                    int transfer_characteristics = U(8, bData, StartBit += 1);
                    int matrix_coefficients = U(8, bData, StartBit += 8);
                }
            }
            int chroma_loc_info_present_flag = U(1, bData, StartBit += 8);
            if (1 == chroma_loc_info_present_flag)
            {
                int chroma_sample_loc_type_top_field = Ue(bData, StartBit += 1);
                int chroma_sample_loc_type_bottom_field = Ue(bData, StartBit += 7);
            }
            int timing_info_present_flag = U(1, bData, StartBit += 7);

            if (1 == timing_info_present_flag)
            {
                int num_units_in_tick = U(32, bData, StartBit += 1);
                int time_scale = U(32, bData, StartBit += 32);
                int fixed_frame_rate_flag = U(1, bData, StartBit += 32);
            }

        }

        // 宽高计算公式
        int width = ((int)pic_width_in_mbs_minus1 + 1) * 16;
        int height = (2 - (int)frame_mbs_only_flag) * ((int)pic_height_in_map_units_minus1 + 1) * 16;

        if (1 == frame_cropping_flag)
        {
            int crop_unit_x;
            int crop_unit_y;
            if (0 == chroma_format_idc) // monochrome
            {
                crop_unit_x = 1;
                crop_unit_y = 2 - frame_mbs_only_flag;
            }
            else if (1 == chroma_format_idc) // 4:2:0
            {
                crop_unit_x = 2;
                crop_unit_y = 2 * (2 - frame_mbs_only_flag);
            }
            else if (2 == chroma_format_idc) // 4:2:2
            {
                crop_unit_x = 2;
                crop_unit_y = 2 - frame_mbs_only_flag;
            }
            else // 3 == sps.chroma_format_idc   // 4:4:4
            {
                crop_unit_x = 1;
                crop_unit_y = 2 - frame_mbs_only_flag;
            }

            width -= crop_unit_x * ((int)frame_crop_left_offset + (int)frame_crop_right_offset);
            height -= crop_unit_y * ((int)frame_crop_top_offset + (int)frame_crop_bottom_offset);
        }

        System.out.println(String.format("width = %6d, height = %6d", width, height));
    }
}
