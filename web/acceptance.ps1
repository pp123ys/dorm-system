# Web 版接口验收脚本（只做只读+可回滚的操作，不改动控制台版）
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$script:pass = 0
$script:fail = 0

function Req($method, $path, $body, $token) {
    $headers = @{}
    if ($token) { $headers['X-Token'] = $token }
    $uri = $base + $path
    try {
        if ($body -ne $null) {
            $json = $body | ConvertTo-Json -Depth 6 -Compress
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
            if ($method -eq 'POST') { $r = Invoke-WebRequest -Uri $uri -Method POST -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $bytes -UseBasicParsing -TimeoutSec 20 }
            elseif ($method -eq 'PUT') { $r = Invoke-WebRequest -Uri $uri -Method PUT -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $bytes -UseBasicParsing -TimeoutSec 20 }
            else { $r = Invoke-WebRequest -Uri $uri -Method $method -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $bytes -UseBasicParsing -TimeoutSec 20 }
        } else {
            $r = Invoke-WebRequest -Uri $uri -Method $method -Headers $headers -UseBasicParsing -TimeoutSec 20
        }
        $text = [System.Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())
        return @{ status = [int]$r.StatusCode; json = ($text | ConvertFrom-Json); raw = $text }
    } catch [System.Net.WebException] {
        $resp = $_.Exception.Response
        if ($resp -ne $null) {
            $sr = New-Object System.IO.StreamReader($resp.GetResponseStream(), [System.Text.Encoding]::UTF8)
            $text = $sr.ReadToEnd()
            $code = [int]$resp.StatusCode
            try { $j = $text | ConvertFrom-Json } catch { $j = $null }
            return @{ status = $code; json = $j; raw = $text }
        }
        return @{ status = -1; json = $null; raw = $_.Exception.Message }
    }
}

function Check($name, $cond, $detail) {
    if ($cond) { $script:pass++; Write-Host ("  [通过] " + $name) }
    else { $script:fail++; Write-Host ("  [失败] " + $name + "   " + $detail) }
}

# ---------------------------------------------------------------------------
# 前置清理：让本脚本可以反复运行。
# t_checkin 是只增不改的流水表（没有删除接口），若上一轮验收留下的记录还在，
# "流水条数"这类计数断言就会失败。这里直连数据库清掉本脚本专用学号的历史流水。
# 脚本只操作学号 2025201 与它自己新建的楼栋，不会碰种子数据和你的业务数据。
# ---------------------------------------------------------------------------
$TEST_NO = 2025201
Write-Host "===== 0. 前置清理（保证脚本可重复运行）====="
$mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
if ($mysqlCmd) {
    & mysql -h 127.0.0.1 -uroot -p123456 --default-character-set=utf8mb4 -e "use dorm_system; delete from t_checkin where student_no = $TEST_NO; delete from t_student where no = $TEST_NO;" 2>&1 | Out-Null
    Write-Host ("  已清理学号 " + $TEST_NO + " 的历史流水与学生记录")
} else {
    Write-Host "  未找到 mysql 客户端：若脚本非首次运行，计数断言可能失败"
}

Write-Host "===== 1. 健康检查与鉴权 ====="
$h = Req GET '/api/health' $null $null
Check 'A3 GET /api/health -> 200 且 code=0' ($h.status -eq 200 -and $h.json.code -eq 0) $h.raw

$noAuth = Req GET '/api/students' $null $null
Check 'A4 无 token 访问 /api/students -> 401' ($noAuth.status -eq 401) ("status=" + $noAuth.status + " " + $noAuth.raw)

$badLogin = Req POST '/api/auth/login' @{ loginName = 'admin'; password = 'wrong' } $null
Check 'A4 错误密码 -> code=1 且提示中文（业务校验失败，非鉴权失败）' ($badLogin.json.code -eq 1 -and $badLogin.json.message -match '密码') ("status=" + $badLogin.status + " " + $badLogin.raw)
$fakeToken = Req GET '/api/students' $null 'not-a-real-token'
Check 'A4 伪造 token -> 401（未登录）' ($fakeToken.status -eq 401) ("status=" + $fakeToken.status + " " + $fakeToken.raw)

$login = Req POST '/api/auth/login' @{ loginName = 'admin'; password = '123456' } $null
Check 'A4 正确密码 -> 拿到 token' ($login.status -eq 200 -and $login.json.data.token.Length -gt 10) $login.raw
$tk = $login.json.data.token
Write-Host ("         token = " + $tk.Substring(0,12) + "...")

$me = Req GET '/api/auth/me' $null $tk
Check '鉴权 GET /api/auth/me -> admin' ($me.status -eq 200 -and $me.json.data.loginName -eq 'admin') $me.raw

Write-Host "===== 2. 楼栋管理 ====="
$bl = Req GET '/api/buildings' $null $tk
Check 'GET /api/buildings -> 2 条种子数据' ($bl.json.code -eq 0 -and $bl.json.data.Count -eq 2) $bl.raw
Check '楼栋名称中文正确' ($bl.json.data[0].name -eq '1号楼') $bl.raw

$newB = Req POST '/api/buildings' @{ name = '3号楼'; sex = '男'; floors = 5; remark = '验收临时' } $tk
Check 'POST /api/buildings 新增' ($newB.json.code -eq 0) $newB.raw
$dupB = Req POST '/api/buildings' @{ name = '3号楼'; sex = '男'; floors = 5; remark = '' } $tk
Check '反例：重名楼栋被拒（走既有校验文案）' ($dupB.json.code -eq 1 -and $dupB.json.message -match '已存在') $dupB.raw
$badSex = Req POST '/api/buildings' @{ name = 'X楼'; sex = '中性'; floors = 3; remark = '' } $tk
Check '反例：非法楼栋类型被拒' ($badSex.json.code -eq 1) $badSex.raw

$bl2 = Req GET '/api/buildings' $null $tk
$newId = ($bl2.json.data | Where-Object { $_.name -eq '3号楼' }).id
Check '新增楼栋可查到' ($newId -ne $null) ($bl2.raw)

$updB = Req PUT ("/api/buildings/" + $newId) @{ name = '3号楼B'; sex = '男'; floors = 4; remark = '已改' } $tk
Check 'PUT /api/buildings/{id} 修改' ($updB.json.code -eq 0) $updB.raw

Write-Host "===== 3. 房间与床位（事务） ====="
$addRoom = Req POST '/api/rooms' @{ buildingId = $newId; roomNo = '301'; capacity = 4 } $tk
Check 'POST /api/rooms（房间+4床位，事务）' ($addRoom.json.code -eq 0) $addRoom.raw

$rooms = Req GET ("/api/buildings/" + $newId + "/rooms") $null $tk
Check 'GET /api/buildings/{id}/rooms -> 1 间' ($rooms.json.data.Count -eq 1) $rooms.raw
$roomId = $rooms.json.data[0].id
Check '房间空床数=4' ($rooms.json.data[0].freeCount -eq 4) $rooms.raw

$beds = Req GET ("/api/rooms/" + $roomId + "/beds") $null $tk
Check 'GET /api/rooms/{id}/beds -> 自动生成 4 个床位' ($beds.json.data.Count -eq 4) $beds.raw

$cap = Req PUT ("/api/rooms/" + $roomId + "/capacity") @{ capacity = 6 } $tk
Check 'PUT 容量 4->6 扩容' ($cap.json.code -eq 0) $cap.raw
$beds2 = Req GET ("/api/rooms/" + $roomId + "/beds") $null $tk
Check '扩容后 6 个床位' ($beds2.json.data.Count -eq 6) $beds2.raw

$capDown = Req PUT ("/api/rooms/" + $roomId + "/capacity") @{ capacity = 2 } $tk
Check 'PUT 容量 6->2 缩容' ($capDown.json.code -eq 0) $capDown.raw
$beds3 = Req GET ("/api/rooms/" + $roomId + "/beds") $null $tk
Check '缩容后 2 个床位' ($beds3.json.data.Count -eq 2) $beds3.raw

Write-Host "===== 4. 学生管理（中文） ====="
$addStu = Req POST '/api/students' @{ no = 2025201; name = '王小明'; sex = '男'; age = 19; phone = '13800000201' } $tk
Check 'A6 POST /api/students 中文新增' ($addStu.json.code -eq 0) $addStu.raw
$stus = Req GET '/api/students' $null $tk
$stu = $stus.json.data | Where-Object { $_.no -eq 2025201 }
Check 'A6 中文姓名回读正确' ($stu.name -eq '王小明') $stus.raw
Check '未入住时 location=未入住' ($stu.location -eq '未入住') $stus.raw

$dupStu = Req POST '/api/students' @{ no = 2025201; name = '李四'; sex = '男'; age = 18; phone = '' } $tk
Check '反例：重复学号被拒' ($dupStu.json.code -eq 1 -and $dupStu.json.message -match '已存在') $dupStu.raw

$updStu = Req PUT '/api/students/2025201' @{ name = '王小明改'; sex = '男'; age = 20; phone = '13900000201' } $tk
Check 'PUT /api/students/{no} 修改' ($updStu.json.code -eq 0) $updStu.raw

Write-Host "===== 5. 入住退住（事务 + 流水） ====="
$fb = Req GET '/api/stays/free-buildings' $null $tk
Check 'GET /api/stays/free-buildings 有数据' ($fb.json.data.Count -ge 1) $fb.raw

$target = Req GET '/api/stays/2025201/check-in-target' $null $tk
Check '入住前预检返回学生' ($target.json.code -eq 0 -and $target.json.data.name -eq '王小明改') $target.raw

$freeBeds = Req GET ("/api/stays/free-beds?buildingId=" + $newId) $null $tk
Check 'GET /api/stays/free-beds 返回空床位' ($freeBeds.json.data.Count -eq 2) $freeBeds.raw
$bedId = $freeBeds.json.data[0].id

$ci = Req POST '/api/stays/check-in' @{ studentNo = 2025201; bedId = $bedId } $tk
Check 'POST /api/stays/check-in 入住' ($ci.json.code -eq 0) $ci.raw

$stay = Req GET '/api/stays/2025201' $null $tk
Check '入住后带出住宿位置' ($stay.json.data.checkedIn -eq $true -and $stay.json.data.location -match '房') $stay.raw
Write-Host ("         location = " + $stay.json.data.location)

$ciAgain = Req POST '/api/stays/check-in' @{ studentNo = 2025201; bedId = $bedId } $tk
Check '反例：同一学生重复入住被拒' ($ciAgain.json.code -eq 1 -and $ciAgain.json.message -match '已入住') $ciAgain.raw

$ciTaken = Req POST '/api/stays/check-in' @{ studentNo = 2025001; bedId = $bedId } $tk
Check '反例：床位已被占用被拒' ($ciTaken.json.code -eq 1) $ciTaken.raw

$hist = Req GET '/api/stats/checkins?studentNo=2025201' $null $tk
Check '流水已写入（入住条+操作人 admin）' ($hist.json.data.Count -eq 1 -and $hist.json.data[0].operator -eq 'admin' -and $hist.json.data[0].action -eq '入住') $hist.raw
Check '流水时间格式 yyyy-MM-dd HH:mm:ss' ($hist.json.data[0].createTime -match '^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$') $hist.raw

$co = Req POST '/api/stays/check-out' @{ studentNo = 2025201 } $tk
Check 'POST /api/stays/check-out 退住' ($co.json.code -eq 0) $co.raw
$hist2 = Req GET '/api/stats/checkins?studentNo=2025201' $null $tk
Check '退住也写入流水（共 2 条）' ($hist2.json.data.Count -eq 2) $hist2.raw

Write-Host "===== 6. 查询统计 ====="
$ov = Req GET '/api/stats/overview' $null $tk
Check 'GET /api/stats/overview' ($ov.json.code -eq 0 -and $ov.json.data.Count -ge 3) $ov.raw
$roster = Req GET ("/api/stats/rooms/" + $roomId + "/roster") $null $tk
Check 'GET /api/stats/rooms/{id}/roster（退住后为空）' ($roster.json.code -eq 0 -and $roster.json.data.Count -eq 0) $roster.raw
$allFree = Req GET '/api/stats/free-beds' $null $tk
Check 'GET /api/stats/free-beds 全部楼栋' ($allFree.json.data.Count -ge 2) $allFree.raw
$sStay = Req GET '/api/stats/students/2025001' $null $tk
Check 'GET /api/stats/students/{no}' ($sStay.json.code -eq 0 -and $sStay.json.data.location -eq '未入住') $sStay.raw
$badStay = Req GET '/api/stats/students/999999' $null $tk
Check '反例：不存在学号返回业务错误' ($badStay.json.code -eq 1) $badStay.raw

Write-Host "===== 7. 清理验收数据并复核 ====="
$existRooms = Req GET ("/api/buildings/" + $newId + "/rooms") $null $tk
$rId = $existRooms.json.data[0].id
$delRoom = Req DELETE ("/api/rooms/" + $rId) $null $tk
Check 'DELETE /api/rooms/{id}（含床位）' ($delRoom.json.code -eq 0) $delRoom.raw
$bedsAfter = Req GET ("/api/rooms/" + $rId + "/beds") $null $tk
Check '删房间后其床位也被删除' ($bedsAfter.json.data.Count -eq 0) $bedsAfter.raw

$delStu = Req DELETE '/api/students/2025201' $null $tk
Check 'DELETE /api/students/{no}' ($delStu.json.code -eq 0) $delStu.raw

$delB = Req DELETE ("/api/buildings/" + $newId) $null $tk
Check 'DELETE /api/buildings/{id}' ($delB.json.code -eq 0) $delB.raw

$final = Req GET '/api/buildings' $null $tk
Check '清理后回到 2 条种子楼栋' ($final.json.data.Count -eq 2) $final.raw
$finalStu = Req GET '/api/students' $null $tk
Check '清理后回到 5 条种子学生' ($finalStu.json.data.Count -eq 5) $finalStu.raw

# 流水表只增不改（没有删除接口），用 mysql 客户端清掉本次验收产生的记录，
# 让验收结束后数据库能完全回到 schema.sql 的种子状态。
Write-Host "===== 8. 清理验收流水（只增表，需直连数据库）====="
$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if ($mysql) {
    $del = & mysql -h 127.0.0.1 -uroot -p123456 --default-character-set=utf8mb4 -e "use dorm_system; delete from t_checkin where student_no = 2025201; select count(*) from t_checkin;" 2>&1
    Write-Host ("  " + ($del -join ' | '))
    Check '流水已清理（t_checkin 回到 0 条）' (($del -join '') -match '(^|\D)0(\D|$)') ($del -join ' ')
} else {
    Write-Host "  未找到 mysql 客户端，跳过；如需清理请手工执行："
    Write-Host "  mysql -h 127.0.0.1 -uroot -p123456 -e ""use dorm_system; delete from t_checkin where student_no = 2025201;"""
}

Write-Host ""
Write-Host ("===== 结果：通过 " + $script:pass + " 项，失败 " + $script:fail + " 项 =====")
if ($script:fail -gt 0) { exit 1 }
